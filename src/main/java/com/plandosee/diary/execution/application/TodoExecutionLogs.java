package com.plandosee.diary.execution.application;

import java.util.List;

import com.plandosee.diary.common.domain.DurationParts;
import com.plandosee.diary.common.paging.PageInfo;
import com.plandosee.diary.execution.domain.ExecutionLogRow;

/**
 * Execution records of one todo: logs is one page in started_at, id order; page says where
 * it sits; actualMinutes is the total of all the todo's records (not just this page), so the page never computes it.
 */
public record TodoExecutionLogs(List<ExecutionLogRow> logs, long actualMinutes, PageInfo page) {

    public TodoExecutionLogs {
        logs = List.copyOf(logs);
    }

    /** actualMinutes split into days, hours and minutes for display. */
    public DurationParts actualDuration() {
        return DurationParts.of(actualMinutes);
    }

    /** All records on one page (unpaged callers). */
    public static TodoExecutionLogs of(List<ExecutionLogRow> logs) {
        return new TodoExecutionLogs(logs, logs.stream().mapToLong(ExecutionLogRow::getActualMinutes).sum(),
                PageInfo.whole(logs.size()));
    }
}
