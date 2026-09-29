package com.plandosee.diary.common.domain;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.plandosee.diary.common.error.DomainRuleException;

/**
 * ADR-29: the one place for the accepted date range of every date and date-time input (plan period, todo due date,
 * execution start and end, read as Seoul local dates). {@value #MIN_TEXT} .. {@value #MAX_TEXT}: four-digit years
 * only, so the browser date input, ISO text without a sign, Java LocalDate and PostgreSQL DATE all write the value
 * the same way, no BC / year 0 values, and an execution period stays far below the int minute limit.
 * The form binding (FormBindingAdvice), the command checks (InputCheck) and the V5 CHECK constraints (ADR-30) use the
 * same bounds with the code {@link FieldCodes#DATE_OUT_OF_RANGE} and arguments {0} = {@value #MIN_TEXT},
 * {1} = {@value #MAX_TEXT}.
 */
public final class DateBounds {

    public static final String MIN_TEXT = "1900-01-01";
    public static final String MAX_TEXT = "2999-12-31";
    public static final LocalDate MIN = LocalDate.parse(MIN_TEXT);
    public static final LocalDate MAX = LocalDate.parse(MAX_TEXT);

    private DateBounds() {
    }

    public static boolean contains(LocalDate date) {
        return date != null && !date.isBefore(MIN) && !date.isAfter(MAX);
    }

    /**
     * A point in time is checked by its local date in {@code zone}. Values Java cannot even place in that zone
     * (OffsetDateTime.MIN / MAX, what the driver reads for -infinity / infinity) are outside.
     */
    public static boolean contains(OffsetDateTime time, ZoneId zone) {
        if (time == null) {
            return false;
        }
        try {
            return contains(time.atZoneSameInstant(zone).toLocalDate());
        } catch (DateTimeException ex) {
            return false;
        }
    }

    /** Null passes (required-field rules report it); outside the range is DomainRuleException(field, code, min, max). */
    public static void check(String field, LocalDate date) {
        if (date != null && !contains(date)) {
            throw outOfRange(field);
        }
    }

    /** A point in time is checked by its local date in {@code zone} (Asia/Seoul for inputs, DEC-02). */
    public static void check(String field, OffsetDateTime time, ZoneId zone) {
        if (time != null && !contains(time, zone)) {
            throw outOfRange(field);
        }
    }

    public static DomainRuleException outOfRange(String field) {
        return new DomainRuleException(field, FieldCodes.DATE_OUT_OF_RANGE, args());
    }

    /** Message arguments {0} and {1}: the bounds as yyyy-MM-dd strings. */
    public static Object[] args() {
        return new Object[] {MIN_TEXT, MAX_TEXT};
    }
}
