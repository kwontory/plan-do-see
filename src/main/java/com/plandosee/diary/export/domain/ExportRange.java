package com.plandosee.diary.export.domain;

import java.time.LocalDate;

import com.plandosee.diary.common.domain.InputCheck;

/**
 * The chosen export range, self-validating: both Seoul dates required and within DateBounds, {@code to} not
 * before {@code from}, and {@code to} at most {@link #MAX_MONTHS} calendar month after {@code from} (month arithmetic
 * with month-end adjustment: 01-31 + 1 month = 02-28/29). Violations are field codes on {@code from} / {@code to}.
 * An invalid range cannot be built, so no file is made from one.
 */
public record ExportRange(LocalDate from, LocalDate to) {

    /** Longest range in calendar months. */
    public static final int MAX_MONTHS = 1;
    public static final String FROM_REQUIRED = "export.range.fromRequired";
    public static final String TO_REQUIRED = "export.range.toRequired";
    public static final String END_BEFORE_START = "export.range.endBeforeStart";
    /** Argument {0}: the limit in months as a plain string. */
    public static final String TOO_LONG = "export.range.tooLong";

    public ExportRange {
        InputCheck check = new InputCheck();
        check.date("from", from, FROM_REQUIRED);
        check.date("to", to, TO_REQUIRED);
        if (check.ok("from") && check.ok("to")) {
            check.rule(!to.isBefore(from), "to", END_BEFORE_START);
            if (check.ok("to")) {
                check.rule(!to.isAfter(latestEnd(from)), "to", TOO_LONG, String.valueOf(MAX_MONTHS));
            }
        }
        check.done();
    }

    /** The last allowed end date for a start date. */
    public static LocalDate latestEnd(LocalDate from) {
        return from.plusMonths(MAX_MONTHS);
    }

    /**
     * The form's default range: ends today, starts about one month earlier. The start is today minus one month, moved
     * later by as few days as needed when month-end adjustment would make the range too long (for 03-31 the start is
     * 03-01, since 02-28 + 1 month is only 03-28).
     */
    public static ExportRange endingOn(LocalDate today) {
        LocalDate from = today.minusMonths(MAX_MONTHS);
        while (latestEnd(from).isBefore(today)) {
            from = from.plusDays(1);
        }
        return new ExportRange(from, today);
    }
}
