package com.plandosee.diary.common.domain;

/**
 * ADR-29: a minute value split into days, hours and minutes for display (a day is 24 hours). The one place where
 * minutes are divided; templates only print these parts with message keys (ADR-13, ADR-17 F-2). Values are stored,
 * summed and exported as whole minutes; this is a view of one such value, never stored.
 * <ul>
 *   <li>totalMinutes: the original value, sign included (for {@code data-value})</li>
 *   <li>days, hours (0..23), minutes (0..59): parts of the absolute value</li>
 *   <li>negative / positive / zero: the sign of totalMinutes (the sign notation is the template's, e.g. variance)</li>
 *   <li>showDays / showHours / showMinutes: which parts to print. Zero parts are left out; a zero total shows only
 *       the minutes part (0 minutes)</li>
 * </ul>
 * Works for every long, including Long.MIN_VALUE (the division is done before taking absolute values).
 */
public record DurationParts(long totalMinutes, long days, int hours, int minutes) {

    public static final int MINUTES_PER_HOUR = 60;
    public static final int HOURS_PER_DAY = 24;
    public static final int MINUTES_PER_DAY = MINUTES_PER_HOUR * HOURS_PER_DAY;

    public static DurationParts of(long totalMinutes) {
        long days = Math.abs(totalMinutes / MINUTES_PER_DAY);
        int rest = (int) Math.abs(totalMinutes % MINUTES_PER_DAY);
        return new DurationParts(totalMinutes, days, rest / MINUTES_PER_HOUR, rest % MINUTES_PER_HOUR);
    }

    public boolean negative() {
        return totalMinutes < 0;
    }

    public boolean positive() {
        return totalMinutes > 0;
    }

    public boolean zero() {
        return totalMinutes == 0;
    }

    public boolean showDays() {
        return days > 0;
    }

    public boolean showHours() {
        return hours > 0;
    }

    public boolean showMinutes() {
        return minutes > 0 || zero();
    }
}
