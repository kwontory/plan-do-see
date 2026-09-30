package com.plandosee.diary.auth.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * The login id as stored in login_attempts: SHA-256 hex (64 lower-case characters) of
 * {@link AuthRules#loginKeySource}. The id text itself is never stored with attempts.
 */
public final class LoginKey {

    private LoginKey() {
    }

    public static String of(String typedLoginId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(AuthRules.loginKeySource(typedLoginId).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
