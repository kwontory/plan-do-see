package com.plandosee.diary.execution.application;

import java.util.List;

import org.springframework.stereotype.Component;

import com.plandosee.diary.common.error.ConstraintCode;
import com.plandosee.diary.common.error.ConstraintCodeSource;
import com.plandosee.diary.common.error.ConstraintViolationTranslator;
import com.plandosee.diary.common.domain.DateBounds;
import com.plandosee.diary.common.domain.FieldCodes;
import com.plandosee.diary.execution.domain.ActualMinutes;

/**
 * Execution record constraints (V1, V4, V5) and the code each means. ExecutionCommand checks the same rules
 * first (DateBounds by Seoul date, ActualMinutes and its length limit, ExecutionRules blocker reason; blank blocker
 * reason stored as NULL).
 */
@Component
public class ExecutionConstraintCodes implements ConstraintCodeSource {

    @Override
    public List<ConstraintCode> constraintCodes() {
        return List.of(
                new ConstraintCode("ck_execution_logs_range", "endedAt", ActualMinutes.END_BEFORE_START),
                new ConstraintCode("ck_execution_logs_actual_minutes", "endedAt", ActualMinutes.END_BEFORE_START),
                new ConstraintCode("ck_execution_logs_blocker_reason", "blockerReason",
                        ConstraintViolationTranslator.VALUE_INVALID),
                new ConstraintCode("ck_execution_logs_blocker_reason_max", "blockerReason",
                        FieldCodes.BLOCKER_REASON_MAX),
                new ConstraintCode("ck_execution_logs_started_at_range", "startedAt", FieldCodes.DATE_OUT_OF_RANGE,
                        DateBounds.args()),
                new ConstraintCode("ck_execution_logs_ended_at_range", "endedAt", FieldCodes.DATE_OUT_OF_RANGE,
                        DateBounds.args()),
                new ConstraintCode("ck_execution_logs_actual_minutes_max", "endedAt", ActualMinutes.TOO_LONG));
    }
}
