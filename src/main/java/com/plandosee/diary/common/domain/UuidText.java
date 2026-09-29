package com.plandosee.diary.common.domain;

import java.util.UUID;

/**
 * A UUID written in the canonical 8-4-4-4-12 hexadecimal form (36 characters, either case). Java's
 * UUID.fromString also accepts shortened groups such as {@code 1-1-1-1-1}; those are not identifiers this app issued.
 * Used for path ids, the completion idempotency key and the tag filter.
 */
public final class UuidText {

    private static final int LENGTH = 36;

    private UuidText() {
    }

    /** The UUID, or null when the text is not in canonical form. */
    public static UUID parse(String text) {
        if (text == null || text.length() != LENGTH) {
            return null;
        }
        for (int i = 0; i < LENGTH; i++) {
            char c = text.charAt(i);
            boolean dash = i == 8 || i == 13 || i == 18 || i == 23;
            if (dash ? c != '-' : !isHex(c)) {
                return null;
            }
        }
        return UUID.fromString(text);
    }

    private static boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }
}
