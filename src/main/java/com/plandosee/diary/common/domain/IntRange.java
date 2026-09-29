package com.plandosee.diary.common.domain;

/**
 * An inclusive whole-number range and the codes for "below" and "above", declared once in the feature's
 * rules class (for example {@code PlanRules.ESTIMATED_MINUTES}) and applied by {@link InputCheck#range}.
 */
public record IntRange(long min, long max, String minCode, String maxCode) {

    public IntRange {
        if (min > max || minCode == null || maxCode == null) {
            throw new IllegalArgumentException("int range");
        }
    }

    public boolean contains(long value) {
        return value >= min && value <= max;
    }
}
