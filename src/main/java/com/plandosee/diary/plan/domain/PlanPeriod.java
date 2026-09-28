package com.plandosee.diary.plan.domain;

import java.time.LocalDate;

import com.plandosee.diary.common.error.DomainRuleException;

/**
 * Plan period rule: the end date cannot be before the start date (a one-day plan is allowed).
 * Used by PlanService and, when other fields fail validation, by the controllers so the period error is shown
 * in the same response (QA-D5).
 */
public final class PlanPeriod {

    public static final String END_BEFORE_START = "plan.period.endBeforeStart";

    private PlanPeriod() {
    }

    public static void check(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new DomainRuleException("endDate", END_BEFORE_START);
        }
    }
}
