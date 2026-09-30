package com.plandosee.diary.auth.domain;

import java.io.Serializable;
import java.security.Principal;
import java.util.Objects;
import java.util.UUID;

/**
 * The logged-in person, whatever the way of logging in. The session keeps only this id; its name is the user id,
 * which is also the session principal index used to end every session of the person.
 */
public record AuthenticatedUser(UUID userId) implements Principal, Serializable {

    public AuthenticatedUser {
        Objects.requireNonNull(userId, "userId");
    }

    @Override
    public String getName() {
        return userId.toString();
    }
}
