package com.plandosee.diary.common.config;

import java.util.UUID;

/**
 * Resolves the owner of every owned read and mutation on the server side: the logged-in person of the current
 * request (auth), never a value the client sent.
 */
public interface CurrentUserProvider {

    UUID currentUserId();
}
