package com.plandosee.diary.execution.domain;

/**
 * Count and actual-minute total of a set of execution records, read with the same conditions as the list (ADR-21).
 */
public class ExecutionLogTotals {

    private long count;
    private long actualMinutes;

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public long getActualMinutes() {
        return actualMinutes;
    }

    public void setActualMinutes(long actualMinutes) {
        this.actualMinutes = actualMinutes;
    }
}
