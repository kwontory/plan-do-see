package com.plandosee.diary.auth.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

/** A new auth_identities row (email columns stay NULL/false for LOCAL). */
public record AuthIdentityRow(UUID id, UUID userId, String provider, String providerUserId, OffsetDateTime linkedAt) {
}
