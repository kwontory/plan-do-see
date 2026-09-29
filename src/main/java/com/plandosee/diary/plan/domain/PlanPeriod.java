package com.plandosee.diary.plan.domain;

import java.time.LocalDate;

import com.plandosee.diary.common.domain.InputCheck;

/**
 * Plan period rule: the end date cannot be before the start date (a one-day plan is allowed). Missing or
 * out-of-range dates are reported by their own rules, not by this one.
 * Shared by PlanCommand (every command, ADR-30) and the form-level constraint {@code @ValidPlanPeriod} on PlanForm,
 * which reports it together with the other field errors in one response (QA-D5, ADR-14 C-3).
 */
public final class PlanPeriod {

    public static final String END_BEFORE_START = "plan.period.endBeforeStart";

    private PlanPeriod() {
    }

    public static boolean isValid(LocalDate startDate, LocalDate endDate) {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }

    /** Checked only when both dates passed their own rules; reported on endDate. */
    public static void check(InputCheck check, LocalDate startDate, LocalDate endDate) {
        if (check.ok("startDate") && check.ok("endDate")) {
            check.rule(isValid(startDate, endDate), "endDate", END_BEFORE_START);
        }
    }
}
