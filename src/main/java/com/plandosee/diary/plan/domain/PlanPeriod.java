package com.plandosee.diary.plan.domain;

import java.time.LocalDate;

import com.plandosee.diary.common.error.DomainRuleException;

/**
 * Plan period rule: the end date cannot be before the start date (a one-day plan is allowed). Missing dates are
 * reported by their own required-field rules, not by this one.
 * Shared by PlanService (defence for direct calls) and the form-level constraint {@code @ValidPlanPeriod} on
 * PlanForm, which reports it together with the other field errors in one response (QA-D5, ADR-14 C-3).
 */
public final class PlanPeriod {

    public static final String END_BEFORE_START = "plan.period.endBeforeStart";

    private PlanPeriod() {
    }

    public static boolean isValid(LocalDate startDate, LocalDate endDate) {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }

    public static void check(LocalDate startDate, LocalDate endDate) {
        if (!isValid(startDate, endDate)) {
            throw new DomainRuleException("endDate", END_BEFORE_START);
        }
    }
}
