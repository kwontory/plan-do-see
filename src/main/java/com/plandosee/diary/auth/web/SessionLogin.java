package com.plandosee.diary.auth.web;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Component;

import com.plandosee.diary.auth.domain.AuthenticatedUser;

/**
 * Logs a person in without a password check, right after a successful sign-up (the password was just set). Same
 * result as the form login: the session id changes (session fixation), the CSRF token is renewed, and a security
 * context holding only AuthenticatedUser is stored through the chain's SecurityContextRepository (server session,
 * principal index = user id).
 */
@Component
public class SessionLogin {

    private final SecurityContextRepository contexts;
    private final CsrfTokenRepository csrfTokens;
    private final SecurityContextHolderStrategy holder = SecurityContextHolder.getContextHolderStrategy();

    public SessionLogin(SecurityContextRepository contexts, CsrfTokenRepository csrfTokens) {
        this.contexts = contexts;
        this.csrfTokens = csrfTokens;
    }

    public void logIn(AuthenticatedUser user, HttpServletRequest request, HttpServletResponse response) {
        HttpSession existing = request.getSession(false);
        if (existing != null) {
            request.changeSessionId();
        }
        csrfTokens.saveToken(null, request, response);
        SecurityContext context = holder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
        holder.setContext(context);
        contexts.saveContext(context, request, response);
    }
}
