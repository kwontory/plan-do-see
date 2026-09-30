package com.plandosee.diary.auth.application;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plandosee.diary.auth.application.port.AuthIdentityMapper;
import com.plandosee.diary.auth.application.port.PasswordCredentialMapper;
import com.plandosee.diary.auth.application.port.UserSessionMapper;
import com.plandosee.diary.auth.domain.AuthCodes;
import com.plandosee.diary.auth.domain.AuthIdentityRow;
import com.plandosee.diary.auth.domain.AuthRules;
import com.plandosee.diary.auth.domain.LocalCredentialRow;
import com.plandosee.diary.auth.domain.LoginAttemptResult;
import com.plandosee.diary.auth.domain.LoginKey;
import com.plandosee.diary.auth.domain.ThrottleRules;
import com.plandosee.diary.common.concurrency.WriteTransactions;
import com.plandosee.diary.common.config.CurrentUserProvider;
import com.plandosee.diary.common.domain.EditOutcome;
import com.plandosee.diary.common.error.ConcurrencyConflictException;
import com.plandosee.diary.common.error.DomainRuleException;
import com.plandosee.diary.common.error.FieldViolation;
import com.plandosee.diary.common.error.NotFoundException;
import com.plandosee.diary.common.id.IdGenerator;
import com.plandosee.diary.execution.application.ExecutionService;
import com.plandosee.diary.plan.application.PlanService;
import com.plandosee.diary.review.application.ReviewService;
import com.plandosee.diary.todo.application.TodoService;
import com.plandosee.diary.user.application.UserService;

/**
 * Sign-up and account settings (ADR-33, ADR-34, ADR-37).
 * <ul>
 *   <li>Sign-up: person (users), LOCAL login (auth_identities) and password hash (password_credentials) in one
 *       transaction; the web layer then logs the new person in. A taken login id is refused by the unique constraint, also when two sign-ups race. Every
 *       failure, whatever the reason, is the one global code {@link AuthCodes#SIGNUP_REJECTED}. Each try from an
 *       address is recorded; past the limit the try is refused without looking at the input.</li>
 *   <li>Password change: the current password is checked (a wrong one counts as a login failure, and blocked tries
 *       are refused), then the new hash is stored and every session of the person is deleted in the same
 *       transaction; the person logs in again.</li>
 * </ul>
 * Passwords are hashed with bcrypt outside any transaction and are never logged or stored as typed.
 */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final UserService userService;
    private final AuthIdentityMapper identities;
    private final PasswordCredentialMapper credentials;
    private final UserSessionMapper sessions;
    private final LoginThrottle throttle;
    private final LoginService loginService;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserProvider currentUserProvider;
    private final IdGenerator idGenerator;
    private final WriteTransactions writes;
    private final Clock clock;
    private final OwnedData ownedData;

    /** The features whose rows an account deletion removes, in foreign-key order (ADR-35, ADR-14 one direction). */
    public record OwnedData(ExecutionService executions, TodoService todos, ReviewService reviews, PlanService plans) {
    }

    public AccountService(UserService userService, AuthIdentityMapper identities, PasswordCredentialMapper credentials,
                          UserSessionMapper sessions, LoginThrottle throttle, LoginService loginService,
                          PasswordEncoder passwordEncoder, CurrentUserProvider currentUserProvider,
                          IdGenerator idGenerator, WriteTransactions writes, Clock clock,
                          ExecutionService executions, TodoService todos, ReviewService reviews, PlanService plans) {
        this.userService = userService;
        this.identities = identities;
        this.credentials = credentials;
        this.sessions = sessions;
        this.throttle = throttle;
        this.loginService = loginService;
        this.passwordEncoder = passwordEncoder;
        this.currentUserProvider = currentUserProvider;
        this.idGenerator = idGenerator;
        this.writes = writes;
        this.clock = clock;
        this.ownedData = new OwnedData(executions, todos, reviews, plans);
    }

    /**
     * Creates the account. Any failure is DomainRuleException(null, {@link AuthCodes#SIGNUP_REJECTED}); a time-budget
     * failure stays ServiceBusyException.
     */
    public SignupResult signup(String loginId, String password, String nickname, String remoteAddr) {
        String clientIp = ThrottleRules.clientIp(remoteAddr);
        if (!throttle.signupAllowed(clientIp)) {
            log.info("event=signup_rejected reason=blocked");
            throw rejected();
        }
        SignupCommand command;
        try {
            command = new SignupCommand(loginId, password, nickname);
        } catch (DomainRuleException invalid) {
            throttle.recordSignup(clientIp, LoginAttemptResult.FAILURE);
            log.info("event=signup_rejected reason=input");
            throw rejected();
        }
        String hash = passwordEncoder.encode(command.password());
        UUID created;
        try {
            created = writes.run(() -> {
                OffsetDateTime now = now();
                UUID userId = userService.create(command.nickname(), now);
                identities.insert(new AuthIdentityRow(idGenerator.newId(), userId, AuthRules.LOCAL_PROVIDER,
                        command.loginId(), now));
                credentials.insert(userId, hash, now);
                return userId;
            });
        } catch (DomainRuleException | ConcurrencyConflictException refused) {
            throttle.recordSignup(clientIp, LoginAttemptResult.FAILURE);
            log.info("event=signup_rejected reason=stored");
            throw rejected();
        }
        throttle.recordSignup(clientIp, LoginAttemptResult.SUCCESS);
        log.info("event=signup_succeeded");
        return new SignupResult(created, command.loginId());
    }

    /** The logged-in person's login id and nickname. */
    @Transactional(readOnly = true)
    public AccountProfile profile() {
        UUID userId = currentUserProvider.currentUserId();
        LocalCredentialRow credential = identities.findLocalCredentialByUserId(userId);
        String nickname = userService.requireActive(userId).getNickname();
        return new AccountProfile(credential == null ? null : credential.getLoginId(), nickname);
    }

    /** The logged-in person's nickname (page header). */
    @Transactional(readOnly = true)
    public String nickname() {
        return userService.requireActive(currentUserProvider.currentUserId()).getNickname();
    }

    /**
     * Saves the logged-in person's nickname: UPDATED, or UNCHANGED when it equals the stored one (nothing written).
     * DomainRuleException("nickname", code) for an invalid one.
     */
    public EditOutcome changeNickname(String nickname) {
        UUID userId = currentUserProvider.currentUserId();
        return writes.run(() -> userService.rename(userId, nickname, now()));
    }

    /**
     * Replaces the logged-in person's password and deletes every session of the person (this one included). Every
     * broken rule is reported at once in one DomainRuleException: "currentPassword"
     * {@link AuthCodes#CURRENT_PASSWORD_MISMATCH} for a wrong or blocked current password (one bcrypt check, done
     * whatever the new password is) and "newPassword" with the password rule code and arguments. Returns how many
     * sessions were ended.
     */
    public int changePassword(String currentPassword, String newPassword, String remoteAddr) {
        UUID userId = currentUserProvider.currentUserId();
        LocalCredentialRow credential = identities.findLocalCredentialByUserId(userId);
        if (credential == null) {
            throw new NotFoundException("credential");
        }
        List<FieldViolation> violations = new ArrayList<>();
        String loginKey = LoginKey.of(credential.getLoginId());
        String clientIp = ThrottleRules.clientIp(remoteAddr);
        if (throttle.loginBlocked(loginKey, clientIp)) {
            log.info("event=password_change_rejected reason=blocked");
            violations.add(new FieldViolation("currentPassword", AuthCodes.CURRENT_PASSWORD_MISMATCH));
        } else if (!loginService.matchesCurrent(credential, currentPassword)) {
            throttle.recordLoginFailure(loginKey, clientIp);
            log.info("event=password_change_rejected reason=current");
            violations.add(new FieldViolation("currentPassword", AuthCodes.CURRENT_PASSWORD_MISMATCH));
        }
        String newCode = AuthRules.passwordCode(newPassword);
        if (newCode != null) {
            violations.add(new FieldViolation("newPassword", newCode, AuthRules.passwordArgs(newCode)));
        }
        if (!violations.isEmpty()) {
            throw new DomainRuleException(violations);
        }
        String hash = passwordEncoder.encode(newPassword);
        int ended = writes.run(() -> {
            if (credentials.updateHash(userId, hash, now()) != 1) {
                throw new NotFoundException("credential");
            }
            return sessions.deleteByPrincipalName(userId.toString());
        });
        log.info("event=password_changed sessionsEnded={}", ended);
        return ended;
    }

    /**
     * Deletes the logged-in person's account and every row of it (ADR-35, T07-C134), after checking the password
     * (a wrong one is recorded as a login failure; blocked tries are refused):
     * DomainRuleException("password", {@link AuthCodes#DELETE_PASSWORD_MISMATCH}) and nothing is deleted. In one
     * transaction, in foreign-key order: execution logs; todo events, revisions, tag links, todos, tags; reviews;
     * plan revisions, plans; the login failures of this login id; every session; the password; the logins; the
     * person. Signup attempts are kept (they carry an address only and cannot be told apart by person).
     */
    public void deleteAccount(String password, String remoteAddr) {
        UUID userId = currentUserProvider.currentUserId();
        LocalCredentialRow credential = identities.findLocalCredentialByUserId(userId);
        if (credential == null) {
            throw new NotFoundException("credential");
        }
        String loginKey = LoginKey.of(credential.getLoginId());
        String clientIp = ThrottleRules.clientIp(remoteAddr);
        if (throttle.loginBlocked(loginKey, clientIp)) {
            log.info("event=account_delete_rejected reason=blocked");
            throw deleteMismatch();
        }
        if (!loginService.matchesCurrent(credential, password)) {
            throttle.recordLoginFailure(loginKey, clientIp);
            log.info("event=account_delete_rejected reason=password");
            throw deleteMismatch();
        }
        writes.run(() -> {
            ownedData.executions().deleteAllOfCurrentUser();
            ownedData.todos().deleteAllOfCurrentUser();
            ownedData.reviews().deleteAllOfCurrentUser();
            ownedData.plans().deleteAllOfCurrentUser();
            throttle.forgetLoginKey(loginKey);
            sessions.deleteByPrincipalName(userId.toString());
            credentials.deleteByUserId(userId);
            identities.deleteByUserId(userId);
            userService.deleteForAccountRemoval(userId);
        });
        log.info("event=account_deleted");
    }

    private static DomainRuleException deleteMismatch() {
        return new DomainRuleException("password", AuthCodes.DELETE_PASSWORD_MISMATCH);
    }

    private static DomainRuleException rejected() {
        return new DomainRuleException(null, AuthCodes.SIGNUP_REJECTED);
    }

    private static DomainRuleException currentMismatch() {
        return new DomainRuleException("currentPassword", AuthCodes.CURRENT_PASSWORD_MISMATCH);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
