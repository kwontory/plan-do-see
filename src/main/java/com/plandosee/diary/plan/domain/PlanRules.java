package com.plandosee.diary.plan.domain;

import java.time.LocalDate;

import com.plandosee.diary.common.domain.FieldCodes;
import com.plandosee.diary.common.domain.FieldRules;
import com.plandosee.diary.common.domain.Priority;

/**
 * ADR-22: the one place for the plan field rules (DEC-07). PlanForm's annotations and PlanService's entry check use
 * these values, and they equal the V1 CHECK constraints (ck_plans_title, ck_plans_success_criteria,
 * ck_plans_estimated_minutes, ck_plans_period); ValidationRulesConsistencyTest compares all three.
 */
public final class PlanRules {

    public static final int TITLE_MAX = 200;
    public static final int SUCCESS_CRITERIA_MAX = 1000;
    public static final int ESTIMATED_MINUTES_MIN = 0;
    public static final int ESTIMATED_MINUTES_MAX = 525_600;

    private PlanRules() {
    }

    /**
     * Throws DomainRuleException(field, code) for the first broken rule, with the same code the form shows
     * (FieldCodes, PlanPeriod.END_BEFORE_START).
     */
    public static void check(String title, LocalDate startDate, LocalDate endDate, Priority priority,
                             String successCriteria, int estimatedMinutes) {
        FieldRules.requireText("title", title, TITLE_MAX, FieldCodes.TITLE_REQUIRED, FieldCodes.TITLE_MAX);
        FieldRules.require("startDate", startDate, FieldCodes.START_DATE_REQUIRED);
        FieldRules.require("endDate", endDate, FieldCodes.END_DATE_REQUIRED);
        PlanPeriod.check(startDate, endDate);
        FieldRules.require("priority", priority, FieldCodes.PRIORITY_REQUIRED);
        FieldRules.requireText("successCriteria", successCriteria, SUCCESS_CRITERIA_MAX,
                FieldCodes.SUCCESS_CRITERIA_REQUIRED, FieldCodes.SUCCESS_CRITERIA_MAX);
        FieldRules.range("estimatedMinutes", estimatedMinutes, ESTIMATED_MINUTES_MIN, ESTIMATED_MINUTES_MAX,
                FieldCodes.ESTIMATED_MINUTES_MIN, FieldCodes.ESTIMATED_MINUTES_MAX);
    }
}
