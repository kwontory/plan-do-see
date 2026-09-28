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
    public static final String ESTIMATED_MINUTES_MIN = "validation.estimatedMinutes.min";
    public static final String ESTIMATED_MINUTES_MAX = "validation.estimatedMinutes.max";
    public static final String IMPROVEMENT_MAX = "validation.improvement.max";
    public static final String BLOCKER_REASON_MAX = "validation.blockerReason.max";

    private FieldCodes() {
    }
}
