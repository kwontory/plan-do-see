package com.plandosee.diary.auth.web;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.stereotype.Component;


/**
 * A write refused by the CSRF check (ADR-38).
 * <ul>
 *   <li>The session the browser referred to does not exist any more (30 minutes idle, logout, password change), or
 *       the browser sent none, and so there is no token: 302 to /login, without a notice (ADR-33 amendment 3). The
 *       page the form was on (same-origin Referer, ReturnPath) is kept in the new session; after logging in the person
 *       goes back to it (GET) and the page script fills in the draft. The write itself is never repeated.</li>
 *   <li>Anything else (a live session with a wrong or missing token): 403 as before.</li>
 * </ul>
 */
@Component
public class SessionEndedHandler implements AccessDeniedHandler {

    /** Session attribute: the app path to open after the next login. */
    static final String RETURN_TO = SessionEndedHandler.class.getName() + ".RETURN_TO";

    private final AccessDeniedHandler forbidden = new AccessDeniedHandlerImpl();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException denied)
            throws IOException, ServletException {
        if (!(denied instanceof MissingCsrfTokenException) || hadLiveSession(request)) {
            forbidden.handle(request, response, denied);
            return;
        }
        String back = ReturnPath.fromReferer(request);
        HttpSession session = request.getSession(true);
        if (back != null) {
            session.setAttribute(RETURN_TO, back);
        }
        response.sendRedirect(request.getContextPath() + AuthFlash.LOGIN_PATH);
    }

    /**
     * True when the session the browser referred to still exists. The CSRF check may already have opened a new empty
     * session for its token, so the new session itself says nothing; the requested session id does.
     */
    private static boolean hadLiveSession(HttpServletRequest request) {
        return request.getRequestedSessionId() != null && request.isRequestedSessionIdValid();
    }
}
