package com.plandosee.diary.review.domain;

import java.math.BigInteger;

/**
 * ADR-27: widths of the review page's estimated/actual bars, as whole percent 0..100 of the larger of the two
 * minute sums. Derived only from a ReviewSummary, so the bars always describe the same numbers as the metric cards
 * and their evidence lists. Values only: labels, units, markup, and how a width is drawn belong to the template.
 *
 * <p>Rule: the larger sum is 100. The other is {@code value * 100 / max} rounded half up (exact integer arithmetic,
 * no overflow for any long). A positive sum that would round to 0 is 1, so a recorded value never draws as empty.
 * A zero sum is 0, and when both sums are 0 both widths are 0. Negative input cannot come from the database (CHECK
 * {@code >= 0}) and is treated as 0.
 */
public record ReviewBars(int estimatedPercent, int actualPercent) {

    public static final int FULL = 100;
    public static final int MIN_VISIBLE = 1;

    private static final BigInteger HUNDRED = BigInteger.valueOf(FULL);
    private static final BigInteger TWO = BigInteger.TWO;

    public static ReviewBars of(long estimatedMinutes, long actualMinutes) {
        long estimated = Math.max(0, estimatedMinutes);
        long actual = Math.max(0, actualMinutes);
        long max = Math.max(estimated, actual);
        return new ReviewBars(percentOf(estimated, max), percentOf(actual, max));
    }

    static int percentOf(long value, long max) {
        if (value <= 0 || max <= 0) {
            return 0;
        }
        if (value >= max) {
            return FULL;
        }
        // round half up: floor((2 * value * 100 + max) / (2 * max))
        BigInteger bigMax = BigInteger.valueOf(max);
        int percent = BigInteger.valueOf(value).multiply(HUNDRED).multiply(TWO).add(bigMax)
                .divide(bigMax.multiply(TWO)).intValueExact();
        return Math.max(MIN_VISIBLE, percent);
    }
}
