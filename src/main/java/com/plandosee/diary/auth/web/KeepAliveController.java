package com.plandosee.diary.auth.web;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * GET /session/keepalive (ADR-38): sent by the page script only while the person is typing. Logged in: 204 and the
 * session's last access time moves to now (Spring Session saves it at the end of the request), nothing else changes.
 * Not logged in (never, or the session ended): 401 without a redirect and without creating a session. Not part of the
 * login blocking (ADR-37).
 */
@Controller
public class KeepAliveController {

    public static final String PATH = "/session/keepalive";

    @GetMapping(PATH)
    public ResponseEntity<Void> keepAlive(Authentication authentication, HttpServletRequest request) {
        if (!AuthController.loggedIn(authentication) || request.getSession(false) == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).cacheControl(CacheControl.noStore()).build();
        }
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
