package com.plandosee.diary.auth.application;

import java.util.UUID;

/** A created account: the new person's id and the stored (lower-case) login id. */
public record SignupResult(UUID userId, String loginId) {
}
