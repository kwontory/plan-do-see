package com.plandosee.diary.execution.application;

import java.util.UUID;

import com.plandosee.diary.common.domain.EditOutcome;

/** Result of an execution record edit: what happened and the todo whose page to go back to (same transaction). */
public record ExecutionEditResult(EditOutcome outcome, UUID todoId) {
}
