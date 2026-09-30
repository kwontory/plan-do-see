package com.plandosee.diary.auth.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.plandosee.diary.auth.domain.AuthenticatedUser;
import com.plandosee.diary.common.config.CurrentUserProvider;

/**
 * The owner of owned data is the authenticated principal of the request's security context (loaded from the server
 * session). Every data path requires login, so a missing principal is a programming error, not a user state.
 */
@Component
public class SecurityContextCurrentUserProvider implements CurrentUserProvider {

    @Override
    public UUID currentUserId() {
        return find().orElseThrow(() -> new AuthenticationCredentialsNotFoundException("no authenticated user"));
    }

    /** The logged-in person of this thread's security context, if any. */
    public static Optional<UUID> find() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return Optional.of(user.userId());
        }
        return Optional.empty();
    }
}
