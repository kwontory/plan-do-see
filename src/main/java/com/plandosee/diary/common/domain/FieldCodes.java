package com.plandosee.diary.common.domain;

/**
 * ADR-22: error codes of the basic field rules shared by several forms. They are the same message keys the form
 * annotations use ({@code {validation.title.max}} and so on), so a rule broken through the form and through a
 * direct service call shows the same notice. The text is written by Frontend in messages.properties.
 */
public final class FieldCodes {

    public static final String TITLE_REQUIRED = "validation.title.required";
    public static final String TITLE_MAX = "validation.title.max";
    public static final String START_DATE_REQUIRED = "validation.startDate.required";
    public static final String END_DATE_REQUIRED = "validation.endDate.required";
    public static final String PRIORITY_REQUIRED = "validation.priority.required";
    public static final String SUCCESS_CRITERIA_REQUIRED = "validation.successCriteria.required";
    public static final String SUCCESS_CRITERIA_MAX = "validation.successCriteria.max";
    public static final String ESTIMATED_MINUTES_REQUIRED = "validation.estimatedMinutes.required";
    public static final String ESTIMATED_MINUTES_MIN = "validation.estimatedMinutes.min";
    public static final String ESTIMATED_MINUTES_MAX = "validation.estimatedMinutes.max";
    public static final String IMPROVEMENT_MAX = "validation.improvement.max";
    public static final String BLOCKER_REASON_MAX = "validation.blockerReason.max";
    /** ADR-29: the days box of the estimated time is not a whole number 0..DurationInput.DAYS_MAX. */
    public static final String ESTIMATED_DAYS_RANGE = "validation.estimatedMinutes.daysRange";
    /** ADR-29: the hours box is not a whole number 0..DurationInput.HOURS_MAX. */
    public static final String ESTIMATED_HOURS_RANGE = "validation.estimatedMinutes.hoursRange";
    /** ADR-29: the minutes box is not a whole number 0..DurationInput.MINUTES_MAX. */
    public static final String ESTIMATED_MINUTES_PART_RANGE = "validation.estimatedMinutes.minutesRange";
    /**
     * ADR-29: a date or date-time outside DateBounds (also a year with five or more digits). Arguments {0} and {1}
     * are the bounds as yyyy-MM-dd.
     */
    public static final String DATE_OUT_OF_RANGE = "validation.date.outOfRange";
    /** ADR-30: a single-line text field (title, tag) contains a line break (LF, CR, NEL, U+2028, U+2029). */
    public static final String TEXT_LINE_BREAK = "validation.text.lineBreak";
    /**
     * ADR-30: a text field contains a control character (NUL and other C0/C1 controls; also TAB in a single-line
     * field). Multi-line fields may contain LF and TAB.
     */
    public static final String TEXT_CONTROL_CHAR = "validation.text.controlChar";
    /**
     * ADR-30 (IV-13): the hidden edit version of an edit form is not a whole number (a changed or broken page). A
     * global form error: the field has no visible box.
     */
    public static final String VERSION_INVALID = "validation.version.invalid";
    /**
     * ADR-30 revised (user decision 2026-09-29): the todo list search text is longer than TodoRules.SEARCH_QUERY_MAX;
     * the list is not searched. Argument {0} is the limit as a plain string.
     */
    public static final String SEARCH_TOO_LONG = "validation.search.tooLong";

    private FieldCodes() {
    }
}
