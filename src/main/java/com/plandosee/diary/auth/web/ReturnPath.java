package com.plandosee.diary.auth.web;

import java.net.URI;
import java.net.URISyntaxException;

import jakarta.servlet.http.HttpServletRequest;

import com.plandosee.diary.common.web.SameOriginWriteFilter;

/**
 * Where to go back after logging in again (ADR-38): only a path inside this app. A value that is not a plain
 * absolute path ({@code /...}), starts with {@code //}, contains a backslash, a control character or a scheme or host,
 * is longer than {@value #MAX_LENGTH}, or points at the login, sign-up, logout or keepalive routes is dropped (the
 * caller then uses /). This prevents an open redirect.
 */
final class ReturnPath {

    static final int MAX_LENGTH = 2000;

    private ReturnPath() {
    }

    /** The path (with query) of a same-origin Referer, if it is a safe return path; otherwise null. */
    static String fromReferer(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || !SameOriginWriteFilter.matchesServer(referer, request)) {
            return null;
        }
        try {
            URI uri = new URI(referer.strip());
            String path = uri.getRawPath();
            String query = uri.getRawQuery();
            String context = request.getContextPath();
            if (path != null && context != null && !context.isEmpty() && path.startsWith(context)) {
                path = path.substring(context.length());
            }
            return safe(query == null ? path : path + "?" + query);
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    /** The value itself when it is a safe app path, otherwise null. */
    static String safe(String candidate) {
        if (candidate == null || candidate.isEmpty() || candidate.length() > MAX_LENGTH
                || !candidate.startsWith("/") || candidate.startsWith("//") || candidate.indexOf('\\') >= 0) {
            return null;
        }
        for (int i = 0; i < candidate.length(); i++) {
            if (Character.isISOControl(candidate.charAt(i)) || Character.isWhitespace(candidate.charAt(i))) {
                return null;
            }
        }
        URI uri;
        try {
            uri = new URI(candidate);
        } catch (URISyntaxException ex) {
            return null;
        }
        if (uri.getScheme() != null || uri.getRawAuthority() != null || uri.getRawPath() == null) {
            return null;
        }
        String path = uri.getRawPath();
        for (String excluded : new String[] {"/login", "/signup", "/logout", "/session/"}) {
            if (path.equals(excluded) || path.startsWith(excluded.endsWith("/") ? excluded : excluded + "/")) {
                return null;
            }
        }
        return candidate;
    }
}
