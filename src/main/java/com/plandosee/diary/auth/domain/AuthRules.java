package com.plandosee.diary.auth.domain;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The one place for the login id and password rules (ADR-33 amendment 2, ADR-34). The sign-up and password-change
 * commands, the form help values and the DB CHECK ck_auth_identities_local_login_id (V6) use these values.
 * <ul>
 *   <li>Login id: {@value #LOGIN_ID_MIN} to {@value #LOGIN_ID_MAX} characters of ASCII letters, digits and underscore
 *       after trimming; upper-case letters are stored and compared in lower case. Never changes after sign-up.</li>
 *   <li>Password: {@value #PASSWORD_MIN} to {@value #PASSWORD_MAX} characters (UTF-16 units, like every other length
 *       in the app) and at most {@value #PASSWORD_MAX_UTF8_BYTES} bytes in UTF-8, because bcrypt ignores what follows
 *       the 72nd byte. Taken exactly as typed: never trimmed or normalized.</li>
 * </ul>
 */
public final class AuthRules {

    public static final int LOGIN_ID_MIN = 4;
    public static final int LOGIN_ID_MAX = 20;
    /** Stored form: lower-case ASCII letters, digits, underscore. Same pattern as the V6 CHECK. */
    public static final Pattern LOGIN_ID =
            Pattern.compile("^[a-z0-9_]{" + LOGIN_ID_MIN + "," + LOGIN_ID_MAX + "}$");
    /** Typed form, before lower-casing (ASCII only, so no non-ASCII letter can lower-case into an ASCII one). */
    private static final Pattern TYPED_LOGIN_ID =
            Pattern.compile("^[A-Za-z0-9_]{" + LOGIN_ID_MIN + "," + LOGIN_ID_MAX + "}$");

    public static final int PASSWORD_MIN = 8;
    public static final int PASSWORD_MAX = 64;
    public static final int PASSWORD_MAX_UTF8_BYTES = 72;

    /** bcrypt cost factor of new hashes (ADR-34). */
    public static final int BCRYPT_STRENGTH = 12;

    /** auth_identities.provider of a login id + password login. */
    public static final String LOCAL_PROVIDER = "LOCAL";

    private AuthRules() {
    }

    /** The stored login id for typed text, or null when the text is not a valid login id. */
    public static String loginId(String typed) {
        if (typed == null) {
            return null;
        }
        String value = typed.strip();
        return TYPED_LOGIN_ID.matcher(value).matches() ? value.toLowerCase(Locale.ROOT) : null;
    }

    /**
     * The value whose hash keys the login-failure records: the typed text trimmed and lower-cased, valid or not, so
     * repeated tries with the same text count together even when it is not a valid login id.
     */
    public static String loginKeySource(String typed) {
        return typed == null ? "" : typed.strip().toLowerCase(Locale.ROOT);
    }

    /** The password code broken by the typed password, or null when it is acceptable. */
    public static String passwordCode(String password) {
        if (password == null || password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
            return AuthCodes.PASSWORD_LENGTH;
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > PASSWORD_MAX_UTF8_BYTES) {
            return AuthCodes.PASSWORD_BYTES;
        }
        return null;
    }

    /** Message arguments of {@link #passwordCode}: {0} min, {1} max for length; {0} bytes for bytes. */
    public static Object[] passwordArgs(String code) {
        return AuthCodes.PASSWORD_BYTES.equals(code)
                ? new Object[] {String.valueOf(PASSWORD_MAX_UTF8_BYTES)}
                : new Object[] {String.valueOf(PASSWORD_MIN), String.valueOf(PASSWORD_MAX)};
    }
}
