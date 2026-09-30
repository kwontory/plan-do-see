package com.plandosee.diary.auth.web;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * After a successful form login: back to the page kept by SessionEndedHandler (checked again with ReturnPath),
 * otherwise /. The kept path is used once.
 */
@Component
public class ReturnAfterLogin implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String target = null;
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object kept = session.getAttribute(SessionEndedHandler.RETURN_TO);
            session.removeAttribute(SessionEndedHandler.RETURN_TO);
            target = kept instanceof String path ? ReturnPath.safe(path) : null;
        }
        response.sendRedirect(request.getContextPath() + (target == null ? "/" : target));
    }
}
