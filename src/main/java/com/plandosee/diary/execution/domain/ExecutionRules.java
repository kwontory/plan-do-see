package com.plandosee.diary.execution.domain;

import com.plandosee.diary.common.domain.FieldCodes;
import com.plandosee.diary.common.domain.FieldRules;

/**
 * ADR-22: the one place for the execution record field rules. ExecutionForm's annotation and ExecutionService's entry
 * check use this value, and it equals the V4 CHECK ck_execution_logs_blocker_reason_max; ValidationRulesConsistencyTest
 * compares them. The length counts LF line breaks as one character, like the other multi-line inputs.
 */
public final class ExecutionRules {

    public static final int BLOCKER_REASON_MAX = 1000;

    private ExecutionRules() {
    }

    /** Optional blocker reason: only the length after strip is checked (blank is stored as NULL). */
    public static void checkBlockerReason(String blockerReason) {
        FieldRules.maxText("blockerReason", blockerReason, BLOCKER_REASON_MAX, FieldCodes.BLOCKER_REASON_MAX);
    }
}
