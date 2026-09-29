package com.plandosee.diary.execution.domain;

import com.plandosee.diary.common.domain.FieldCodes;
import com.plandosee.diary.common.domain.TextInput;
import com.plandosee.diary.common.domain.TextRule;

/**
 * ADR-22 / ADR-30: the one place for the execution record field rules. ExecutionCommand checks every record with
 * them, ExecutionForm's annotations use the same constants, and they equal the CHECK constraints (V4
 * ck_execution_logs_blocker_reason_max; V5 ck_execution_logs_actual_minutes_max and the Seoul-date ranges of
 * started_at / ended_at); ValidationRulesConsistencyTest compares them. The blocker length counts LF line breaks as one
 * character, like the other multi-line inputs; blank or invisible-only text is stored as NULL.
 */
public final class ExecutionRules {

    public static final int BLOCKER_REASON_MAX = 1000;
    /**
     * ADR-30 (IV-02): longest single execution record in whole minutes, 365 days: the same as the longest estimate
     * (PlanRules / TodoRules ESTIMATED_MINUTES_MAX). Longer is {@link ActualMinutes#TOO_LONG}.
     */
    public static final int PERIOD_MAX_MINUTES = 525_600;

    public static final TextRule BLOCKER_REASON =
            TextRule.optional(TextInput.Lines.MULTI, BLOCKER_REASON_MAX, FieldCodes.BLOCKER_REASON_MAX);

    private ExecutionRules() {
    }
}
