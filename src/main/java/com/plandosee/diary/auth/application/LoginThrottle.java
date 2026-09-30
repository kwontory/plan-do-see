package com.plandosee.diary.auth.application;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plandosee.diary.auth.application.port.LoginAttemptMapper;
import com.plandosee.diary.auth.domain.LoginAttemptKind;
import com.plandosee.diary.auth.domain.LoginAttemptResult;
import com.plandosee.diary.auth.domain.LoginAttemptRow;
import com.plandosee.diary.auth.domain.ThrottleRules;
import com.plandosee.diary.common.id.IdGenerator;

/**
 * Brute-force blocking with the login_attempts table (ADR-37, ThrottleRules). The table, not memory, holds the
 * counts, so every application instance sees the same numbers. Times come from the injected Clock. Each method is its
 * own short transaction: a failure is recorded even though the login itself fails.
 */
@Service
public class LoginThrottle {

    private final LoginAttemptMapper attempts;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final AtomicReference<Instant> lastCleanup = new AtomicReference<>(Instant.MIN);

    public LoginThrottle(LoginAttemptMapper attempts, IdGenerator idGenerator, Clock clock) {
        this.attempts = attempts;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    /** True while this login key from this address, or this address alone, has too many recent failures. */
    @Transactional(readOnly = true)
    public boolean loginBlocked(String loginKeyHash, String clientIp) {
        OffsetDateTime since = now().minus(ThrottleRules.LOGIN_WINDOW);
        return attempts.countLoginFailuresByKeyAndIp(loginKeyHash, clientIp, since,
                        ThrottleRules.FAILURES_PER_ID_AND_IP) >= ThrottleRules.FAILURES_PER_ID_AND_IP
                || attempts.countLoginFailuresByIp(clientIp, since, ThrottleRules.FAILURES_PER_IP)
                        >= ThrottleRules.FAILURES_PER_IP;
    }

    @Transactional
    public void recordLoginFailure(String loginKeyHash, String clientIp) {
        record(LoginAttemptKind.LOGIN, loginKeyHash, clientIp, LoginAttemptResult.FAILURE);
    }

    /** After a successful login: the failures of this login key from this address are forgotten. */
    @Transactional
    public void clearLoginFailures(String loginKeyHash, String clientIp) {
        attempts.deleteLoginFailures(loginKeyHash, clientIp);
    }

    /** Account deletion: every login failure of this login key, from any address, is deleted. */
    @Transactional
    public void forgetLoginKey(String loginKeyHash) {
        attempts.deleteByLoginKey(loginKeyHash);
    }

    /** True while this address may still try to sign up. */
    @Transactional(readOnly = true)
    public boolean signupAllowed(String clientIp) {
        OffsetDateTime since = now().minus(ThrottleRules.SIGNUP_WINDOW);
        return attempts.countSignupsByIp(clientIp, since, ThrottleRules.SIGNUPS_PER_IP) < ThrottleRules.SIGNUPS_PER_IP;
    }

    @Transactional
    public void recordSignup(String clientIp, LoginAttemptResult result) {
        record(LoginAttemptKind.SIGNUP, null, clientIp, result);
    }

    /** Deletes records older than ThrottleRules.RETENTION (one batch); returns how many. */
    @Transactional
    public int purgeExpired() {
        return attempts.deleteOlderThan(now().minus(ThrottleRules.RETENTION), ThrottleRules.CLEANUP_BATCH);
    }

    private void record(LoginAttemptKind kind, String loginKeyHash, String clientIp, LoginAttemptResult result) {
        OffsetDateTime now = now();
        attempts.insert(new LoginAttemptRow(idGenerator.newId(), kind, loginKeyHash, clientIp, result, now));
        Instant previous = lastCleanup.get();
        Instant current = now.toInstant();
        boolean due = !current.isBefore(previous.plus(ThrottleRules.CLEANUP_INTERVAL)) || current.isBefore(previous);
        if (due) {
            if (lastCleanup.compareAndSet(previous, current)) {
                attempts.deleteOlderThan(now.minus(ThrottleRules.RETENTION), ThrottleRules.CLEANUP_BATCH);
            }
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
