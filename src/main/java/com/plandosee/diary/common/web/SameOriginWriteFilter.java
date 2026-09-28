package com.plandosee.diary.common.web;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * T06 CSRF mitigation (ADR-09): state-changing requests from a foreign browser origin are rejected.
 * T07 replaces this with Spring Security CSRF tokens.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SameOriginWriteFilter extends OncePerRequestFilter {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (SAFE_METHODS.contains(request.getMethod()) || isSameOrigin(request)) {
            chain.doFilter(request, response);
            return;
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    private boolean isSameOrigin(HttpServletRequest request) {
        String source = request.getHeader("Origin");
        if (source == null || source.isBlank() || "null".equals(source)) {
            source = request.getHeader("Referer");
        }
        if (source == null || source.isBlank()) {
            return true;
        }
        String sourceHost;
        try {
            sourceHost = URI.create(source.trim()).getHost();
        } catch (IllegalArgumentException ex) {
            return false;
        }
        String requestHost = forwardedOrServerHost(request);
        return sourceHost != null && sourceHost.equalsIgnoreCase(requestHost);
    }

    private String forwardedOrServerHost(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-Host");
        String host = forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : request.getHeader("Host");
        if (host == null || host.isBlank()) {
            return request.getServerName();
        }
        int colon = host.lastIndexOf(':');
        return colon > 0 && !host.endsWith("]") ? host.substring(0, colon) : host;
    }
}
