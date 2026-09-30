package com.plandosee.diary.auth.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

/** One login_attempts row. loginKeyHash is set for LOGIN and null for SIGNUP. */
public record LoginAttemptRow(UUID id, LoginAttemptKind kind, String loginKeyHash, String clientIp,
                              LoginAttemptResult result, OffsetDateTime attemptedAt) {
}
