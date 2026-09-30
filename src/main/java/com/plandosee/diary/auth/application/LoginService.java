package com.plandosee.diary.auth.application;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import com.plandosee.diary.auth.application.port.AuthIdentityMapper;
import com.plandosee.diary.auth.domain.AuthRules;
import com.plandosee.diary.auth.domain.AuthenticatedUser;
import com.plandosee.diary.auth.domain.LocalCredentialRow;
import com.plandosee.diary.auth.domain.LoginKey;
import com.plandosee.diary.auth.domain.ThrottleRules;

/**
 * Login id + password check (ADR-33, ADR-34, ADR-37). The answer is only "this person" or "no", never why.
 * <ol>
 *   <li>Blocked login key + address, or blocked address: no, without checking the password (no bcrypt).</li>
 *   <li>Unknown login id, deleted person, or a password outside the rules: a bcrypt check against a fixed hash
 *       anyway, so the answer takes about as long as a wrong password; recorded as a failure.</li>
 *   <li>Wrong password: recorded as a failure.</li>
 *   <li>Right password: the failures of this login key from this address are forgotten, last_login_at is set.</li>
 * </ol>
 * No transaction spans the bcrypt check. The log line has the outcome kind only (never the id, password or
 * address).
 */
@Service
public class LoginService {

    private static final Logger log = LoggerFactory.getLogger(LoginService.class);
    private static final String TIMING_INPUT = "fixed-input-for-equal-timing";

    private final AuthIdentityMapper identities;
    private final LoginThrottle throttle;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final TransactionTemplate transaction;
    private final String timingHash;

    public LoginService(AuthIdentityMapper identities, LoginThrottle throttle, PasswordEncoder passwordEncoder,
                        Clock clock, PlatformTransactionManager transactionManager) {
        this.identities = identities;
        this.throttle = throttle;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.transaction = new TransactionTemplate(transactionManager);
        this.timingHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    /** The person when the login id and password match and nothing blocks the try; empty otherwise. */
    public Optional<AuthenticatedUser> authenticate(String typedLoginId, String password, String remoteAddr) {
        String loginKey = LoginKey.of(typedLoginId);
        String clientIp = ThrottleRules.clientIp(remoteAddr);
        if (throttle.loginBlocked(loginKey, clientIp)) {
            log.info("event=login_failed reason=blocked");
            return Optional.empty();
        }
        String loginId = AuthRules.loginId(typedLoginId);
        LocalCredentialRow credential = loginId == null ? null
                : transaction.execute(status -> identities.findLocalCredentialByLoginId(loginId));
        boolean matches;
        if (credential == null || AuthRules.passwordCode(password) != null) {
            passwordEncoder.matches(TIMING_INPUT, timingHash);
            matches = false;
        } else {
            matches = passwordEncoder.matches(password, credential.getPasswordHash());
        }
        if (!matches) {
            throttle.recordLoginFailure(loginKey, clientIp);
            log.info("event=login_failed reason=credentials");
            return Optional.empty();
        }
        OffsetDateTime now = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        transaction.executeWithoutResult(status -> {
            throttle.clearLoginFailures(loginKey, clientIp);
            identities.touchLastLogin(credential.getIdentityId(), now);
        });
        log.info("event=login_succeeded");
        return Optional.of(new AuthenticatedUser(credential.getUserId()));
    }

    /** True when the password is the person's current password (bcrypt check). */
    boolean matchesCurrent(LocalCredentialRow credential, String password) {
        if (AuthRules.passwordCode(password) != null) {
            passwordEncoder.matches(TIMING_INPUT, timingHash);
            return false;
        }
        return passwordEncoder.matches(password, credential.getPasswordHash());
    }
}
