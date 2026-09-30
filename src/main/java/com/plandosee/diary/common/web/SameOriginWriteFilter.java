package com.plandosee.diary.common.web;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
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
 * CSRF mitigation: a state-changing request is let through only when its browser
 * origin is exactly this server's origin (scheme, host, and port).
 * <ul>
 * <li>{@code Origin} present: it must be a same-origin {@code http(s)://host[:port]}. {@code Origin: null} (opaque
 * origin: a page with {@code Referrer-Policy: no-referrer}, a sandboxed frame, a {@code data:} or file page), a blank
 * value, or anything unparsable is rejected.</li>
 * <li>{@code Origin} absent: the {@code Referer} decides the same way.</li>
 * <li>Both absent: allowed here (non-browser clients); the Spring Security CSRF token is still required.</li>
 * </ul>
 * The server's origin is {@code request.getScheme()/getServerName()/getServerPort()} only. This filter never reads
 * {@code X-Forwarded-*} itself; behind a proxy those values come from the container's standard forwarded-header
 * handling ({@code server.forward-headers-strategy=native}, Tomcat RemoteIpValve), which applies them only when the
 * connection comes from a trusted internal proxy. Runs before the Spring Security chain, whose session-stored CSRF
 * token is the second check on the same requests.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
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

    static boolean isSameOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin != null) {
            return matchesServer(origin, request);
        }
        String referer = request.getHeader("Referer");
        if (referer != null) {
            return matchesServer(referer, request);
        }
        return true;
    }

    /** True only for an absolute http(s) URL whose scheme, host, and effective port equal the server's. */
    public static boolean matchesServer(String source, HttpServletRequest request) {
        URI uri;
        try {
            uri = new URI(source.strip());
        } catch (URISyntaxException ex) {
            return false;
        }
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null) {
            return false;
        }
        scheme = scheme.toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            return false;
        }
        String serverScheme = request.getScheme() == null ? "" : request.getScheme().toLowerCase(Locale.ROOT);
        return scheme.equals(serverScheme)
                && bare(host).equalsIgnoreCase(bare(request.getServerName()))
                && effectivePort(scheme, uri.getPort()) == effectivePort(serverScheme, request.getServerPort());
    }

    private static int effectivePort(String scheme, int port) {
        if (port > 0) {
            return port;
        }
        return "https".equals(scheme) ? 443 : 80;
    }

    /** IPv6 literals may come with or without brackets. */
    private static String bare(String host) {
        if (host == null) {
            return "";
        }
        return host.startsWith("[") && host.endsWith("]") ? host.substring(1, host.length() - 1) : host;
    }
}
