package com.plandosee.diary.execution.domain;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import com.plandosee.diary.common.error.DomainRuleException;
import com.plandosee.diary.common.time.TimeConfig;

/**
 * No two execution records of one person may overlap. Periods are half-open {@code [start, end)}: a record
 * ending when the next starts is fine, and a record with start = end (0 minutes) never overlaps anything.
 */
public final class ExecutionOverlap {

    /**
     * The period overlaps one of the person's other records. Arguments: {0} that record's todo title, {1} its start,
     * {2} its end, both Asia/Seoul {@code yyyy-MM-dd HH:mm:ss} (the display format of times). Reported on startedAt.
     */
    public static final String OVERLAPS = "execution.time.overlaps";

    private static final DateTimeFormatter SHOWN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ExecutionOverlap() {
    }

    /** True when the period has a length, so it can overlap at all. */
    public static boolean hasLength(OffsetDateTime startedAt, OffsetDateTime endedAt) {
        return startedAt.isBefore(endedAt);
    }

    public static DomainRuleException rejected(ExecutionLogRow other) {
        return new DomainRuleException("startedAt", OVERLAPS, other.getTodoTitle(), shown(other.getStartedAt()),
                shown(other.getEndedAt()));
    }

    private static String shown(OffsetDateTime time) {
        return time == null ? "" : SHOWN.format(time.atZoneSameInstant(TimeConfig.SEOUL));
    }
}
