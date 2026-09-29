package com.plandosee.diary.common.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * The one place for the day/hour/minute input rule of an estimated time. The form has three boxes; the
 * server combines them as {@code days * 1440 + hours * 60 + minutes} and stores whole minutes.
 * <ul>
 *   <li>An empty box is 0. All three empty is "required" ({@link FieldCodes#ESTIMATED_MINUTES_REQUIRED}).</li>
 *   <li>Each box is a whole number without sign: days 0..{@value #DAYS_MAX}, hours 0..{@value #HOURS_MAX}, minutes
 *       0..{@value #MINUTES_MAX}. Anything else (negative, decimal, letters, too many digits) is that box's range
 *       code.</li>
 *   <li>The total must not exceed the feature's maximum (PlanRules / TodoRules ESTIMATED_MINUTES_MAX,
 *       {@link FieldCodes#ESTIMATED_MINUTES_MAX}).</li>
 * </ul>
 * One error for the group: the first broken rule in the order required, days, hours, minutes, total. invalidParts
 * names every box that breaks its own rule (for aria-invalid); required and total errors mark all three.
 * Each input is expected already trimmed with blank as null (FormBindingAdvice); a blank string is also empty here.
 */
public final class DurationInput {

    public static final int DAYS_MAX = 365;
    public static final int HOURS_MAX = DurationParts.HOURS_PER_DAY - 1;
    public static final int MINUTES_MAX = DurationParts.MINUTES_PER_HOUR - 1;

    /** Longest digit string accepted for one box; longer input is out of range without parsing. */
    private static final int MAX_DIGITS = 9;

    public enum Part {
        DAYS(DAYS_MAX, FieldCodes.ESTIMATED_DAYS_RANGE),
        HOURS(HOURS_MAX, FieldCodes.ESTIMATED_HOURS_RANGE),
        MINUTES(MINUTES_MAX, FieldCodes.ESTIMATED_MINUTES_PART_RANGE);

        private final int max;
        private final String code;

        Part(int max, String code) {
            this.max = max;
            this.code = code;
        }

        public int max() {
            return max;
        }

        public String code() {
            return code;
        }
    }

    /**
     * totalMinutes is set only when valid; code is the one group error otherwise.
     */
    public record Result(Integer totalMinutes, String code, Set<Part> invalidParts) {

        public boolean valid() {
            return code == null;
        }

        public boolean invalid(Part part) {
            return invalidParts.contains(part);
        }
    }

    private DurationInput() {
    }

    public static Result combine(String days, String hours, String minutes, int totalMax) {
        if (isEmpty(days) && isEmpty(hours) && isEmpty(minutes)) {
            return new Result(null, FieldCodes.ESTIMATED_MINUTES_REQUIRED, EnumSet.allOf(Part.class));
        }
        Integer d = part(days, Part.DAYS);
        Integer h = part(hours, Part.HOURS);
        Integer m = part(minutes, Part.MINUTES);
        Set<Part> invalid = EnumSet.noneOf(Part.class);
        if (d == null) {
            invalid.add(Part.DAYS);
        }
        if (h == null) {
            invalid.add(Part.HOURS);
        }
        if (m == null) {
            invalid.add(Part.MINUTES);
        }
        if (!invalid.isEmpty()) {
            return new Result(null, invalid.iterator().next().code(), invalid);
        }
        long total = toMinutes(d, h, m);
        if (total > totalMax) {
            return new Result(null, FieldCodes.ESTIMATED_MINUTES_MAX, EnumSet.allOf(Part.class));
        }
        return new Result((int) total, null, EnumSet.noneOf(Part.class));
    }

    public static long toMinutes(long days, long hours, long minutes) {
        return days * DurationParts.MINUTES_PER_DAY + hours * DurationParts.MINUTES_PER_HOUR + minutes;
    }

    /** The value of one box, 0 when empty, null when it breaks the box rule (NumberText: ASCII digits only). */
    private static Integer part(String text, Part part) {
        if (isEmpty(text)) {
            return 0;
        }
        Long number = NumberText.unsigned(text.strip(), MAX_DIGITS);
        return number != null && number <= part.max() ? number.intValue() : null;
    }

    private static boolean isEmpty(String text) {
        return text == null || text.isBlank();
    }
}
