package com.plandosee.diary.common.web;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * ADR-26 security response headers on every response (pages, redirects, downloads, error pages, static files).
 * The app has no inline script or style, no external request, no image, and self-hosted fonts, so
 * {@code default-src 'self'} needs no extra source. Everything except the static resource paths is
 * {@code Cache-Control: no-store} (pages show the user's data); static files keep the default caching.
 * Runs on the error dispatch too, so container error pages (403 from SameOriginWriteFilter, 404, 500) carry them.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    public static final String CONTENT_SECURITY_POLICY = "default-src 'self'; frame-ancestors 'none'; "
            + "form-action 'self'; base-uri 'none'; object-src 'none'";
    public static final String REFERRER_POLICY = "same-origin";
    public static final String NO_STORE = "no-store";

    /** Paths served by the static resource handler (src/main/resources/static). */
    static final List<String> STATIC_PREFIXES = List.of("/css/", "/js/", "/fonts/");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("Content-Security-Policy", CONTENT_SECURITY_POLICY);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", REFERRER_POLICY);
        if (!isStatic(request)) {
            response.setHeader("Cache-Control", NO_STORE);
        }
        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    static boolean isStatic(HttpServletRequest request) {
        String path = request.getRequestURI();
        String context = request.getContextPath();
        if (path == null) {
            return false;
        }
        if (context != null && !context.isEmpty() && path.startsWith(context)) {
            path = path.substring(context.length());
        }
        for (String prefix : STATIC_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
