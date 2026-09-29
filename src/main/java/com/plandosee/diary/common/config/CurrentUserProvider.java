package com.plandosee.diary.common.config;

import java.util.UUID;

/**
 * Resolves the owner of every owned read and mutation on the server side.
 * Returns the configured demo user; once login exists, the authenticated principal replaces it.
 */
public interface CurrentUserProvider {

    UUID currentUserId();
}
