package com.plandosee.diary.execution.domain;

import java.time.Duration;
import java.time.OffsetDateTime;

import com.plandosee.diary.common.domain.InputCheck;
import com.plandosee.diary.common.error.DomainRuleException;

/**
 * Actual minutes are computed by the server from the elapsed time and rounded up so work is never
 * under-counted. Exactly zero elapsed time is 0 minutes; 1..60 seconds is 1 minute; 61 seconds is 2 minutes.
 * One record lasts at most {@link ExecutionRules#PERIOD_MAX_MINUTES} minutes ({@link #TOO_LONG}).
 */
public final class ActualMinutes {

    public static final String STARTED_AT_REQUIRED = "execution.startedAt.required";
    public static final String ENDED_AT_REQUIRED = "execution.endedAt.required";
    public static final String END_BEFORE_START = "execution.period.endBeforeStart";
    /** The record is longer than ExecutionRules.PERIOD_MAX_MINUTES (365 days). */
    public static final String TOO_LONG = "execution.period.tooLong";

    private ActualMinutes() {
    }

    public static int between(OffsetDateTime startedAt, OffsetDateTime endedAt) {
        if (startedAt == null) {
            throw new DomainRuleException("startedAt", STARTED_AT_REQUIRED);
        }
        if (endedAt == null) {
            throw new DomainRuleException("endedAt", ENDED_AT_REQUIRED);
        }
        InputCheck check = new InputCheck();
        int minutes = check(check, startedAt, endedAt);
        check.done();
        return minutes;
    }

    /**
     * The period rule on two times that passed their own rules: end before start is {@link #END_BEFORE_START}, longer
     * than the limit is {@link #TOO_LONG}, both on endedAt. Returns the minutes (0 when broken).
     */
    public static int check(InputCheck check, OffsetDateTime startedAt, OffsetDateTime endedAt) {
        Duration elapsed = Duration.between(startedAt, endedAt);
        if (elapsed.isNegative()) {
            check.reject("endedAt", END_BEFORE_START);
            return 0;
        }
        long minutes = roundedUp(elapsed);
        if (minutes > ExecutionRules.PERIOD_MAX_MINUTES) {
            check.reject("endedAt", TOO_LONG);
            return 0;
        }
        return (int) minutes;
    }

    static int ofElapsed(Duration elapsed) {
        if (elapsed.isNegative()) {
            throw new DomainRuleException("endedAt", END_BEFORE_START);
        }
        long minutes = roundedUp(elapsed);
        if (minutes > ExecutionRules.PERIOD_MAX_MINUTES) {
            throw new DomainRuleException("endedAt", TOO_LONG);
        }
        return (int) minutes;
    }

    private static long roundedUp(Duration elapsed) {
        long wholeMinutes = elapsed.toMinutes();
        boolean remainder = elapsed.minusMinutes(wholeMinutes).compareTo(Duration.ZERO) > 0;
        return wholeMinutes + (remainder ? 1 : 0);
    }
}
