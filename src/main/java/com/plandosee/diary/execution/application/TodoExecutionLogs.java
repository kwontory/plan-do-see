package com.plandosee.diary.execution.application;

import java.util.List;

import com.plandosee.diary.execution.domain.ExecutionLogRow;

/**
 * S03 execution records of one todo and their total (ADR-17 F-2): actualMinutes is the sum of exactly these rows,
 * so the page never computes it.
 */
public record TodoExecutionLogs(List<ExecutionLogRow> logs, long actualMinutes) {

    public static TodoExecutionLogs of(List<ExecutionLogRow> logs) {
        return new TodoExecutionLogs(List.copyOf(logs), logs.stream().mapToLong(ExecutionLogRow::getActualMinutes).sum());
    }
}
