package com.plandosee.diary.common.config;

import java.util.UUID;

/**
 * Resolves the owner of every owned read and mutation on the server side.
 * T06 returns the configured demo user; T07 replaces it with the authenticated principal.
 */
public interface CurrentUserProvider {

    UUID currentUserId();
}
