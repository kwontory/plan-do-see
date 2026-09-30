package com.plandosee.diary.auth.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * The per-person part of the browser's draft keys (ADR-38): the first {@value #LENGTH} hex characters of
 * SHA-256("pds-draft:" + user id). Stable for a person, different between people, and not the user id itself, so a
 * draft typed by one account is never filled in for another account in the same tab.
 */
public final class DraftOwnerKey {

    public static final int LENGTH = 32;
    private static final String PREFIX = "pds-draft:";

    private DraftOwnerKey() {
    }

    public static String of(UUID userId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((PREFIX + userId).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, LENGTH);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
