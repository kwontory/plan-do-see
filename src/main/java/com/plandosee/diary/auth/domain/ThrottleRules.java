package com.plandosee.diary.auth.domain;

import java.time.Duration;

/**
 * Brute-force limits (ADR-37). A limit holds while the number of counted attempts in the window just before now
 * reaches it; attempts refused because of a limit are not counted, so rapid failures block for about one window
 * after the first of them.
 * <ul>
 *   <li>Login, same login id and same IP: {@value #FAILURES_PER_ID_AND_IP} failures in {@link #LOGIN_WINDOW}.</li>
 *   <li>Login, same IP, any login id: {@value #FAILURES_PER_IP} failures in {@link #LOGIN_WINDOW}.</li>
 *   <li>Sign-up, same IP: {@value #SIGNUPS_PER_IP} attempts (created or not) in {@link #SIGNUP_WINDOW}.</li>
 * </ul>
 * A login id alone is never blocked, so nobody can lock someone else out from another address. Records older than
 * {@link #RETENTION} are deleted, at most once per {@link #CLEANUP_INTERVAL} per application instance.
 */
public final class ThrottleRules {

    public static final Duration LOGIN_WINDOW = Duration.ofMinutes(15);
    public static final int FAILURES_PER_ID_AND_IP = 5;
    public static final int FAILURES_PER_IP = 30;
    public static final Duration SIGNUP_WINDOW = Duration.ofHours(1);
    public static final int SIGNUPS_PER_IP = 10;
    public static final Duration RETENTION = Duration.ofDays(1);
    public static final Duration CLEANUP_INTERVAL = Duration.ofMinutes(5);
    public static final int CLEANUP_BATCH = 1000;
    /** Longest stored client address (V6 ck_login_attempts_client_ip). */
    public static final int CLIENT_IP_MAX = 100;
    /** Stored when the container gives no address. */
    public static final String UNKNOWN_IP = "unknown";

    private ThrottleRules() {
    }

    /** The address to store: the container's remote address, trimmed and at most {@value #CLIENT_IP_MAX} long. */
    public static String clientIp(String remoteAddr) {
        if (remoteAddr == null || remoteAddr.isBlank()) {
            return UNKNOWN_IP;
        }
        String value = remoteAddr.strip();
        return value.length() > CLIENT_IP_MAX ? value.substring(0, CLIENT_IP_MAX) : value;
    }
}
