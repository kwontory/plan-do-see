package com.plandosee.diary.common.domain;

import com.plandosee.diary.common.error.DomainRuleException;

/**
 * ADR-22 building blocks for the service-side field checks. Lengths are counted like the form's {@code @Size}
 * (String.length() after strip), which is never looser than the DB's char_length(btrim(...)).
 */
public final class FieldRules {

    private FieldRules() {
    }

    /** Required text: null or blank after strip is {@code requiredCode}; longer than max is {@code maxCode}. */
    public static void requireText(String field, String value, int max, String requiredCode, String maxCode) {
        if (value == null || value.isBlank()) {
            throw new DomainRuleException(field, requiredCode);
        }
        maxText(field, value, max, maxCode);
    }

    /** Optional text: only the length is checked. */
    public static void maxText(String field, String value, int max, String maxCode) {
        if (value != null && value.strip().length() > max) {
            throw new DomainRuleException(field, maxCode);
        }
    }

    public static void require(String field, Object value, String requiredCode) {
        if (value == null) {
            throw new DomainRuleException(field, requiredCode);
        }
    }

    public static void range(String field, long value, long min, long max, String minCode, String maxCode) {
        if (value < min) {
            throw new DomainRuleException(field, minCode);
        }
        if (value > max) {
            throw new DomainRuleException(field, maxCode);
        }
    }
}
