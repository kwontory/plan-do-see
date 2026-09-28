package com.plandosee.diary.execution.domain;

import java.time.Duration;
import java.time.OffsetDateTime;

import com.plandosee.diary.common.error.DomainRuleException;

/**
 * DEC-03: actual minutes are computed by the server from the elapsed time and rounded up so work is never
 * under-counted. Exactly zero elapsed time is 0 minutes; 1..60 seconds is 1 minute; 61 seconds is 2 minutes.
 */
public final class ActualMinutes {

    public static final String STARTED_AT_REQUIRED = "execution.startedAt.required";
    public static final String ENDED_AT_REQUIRED = "execution.endedAt.required";
    public static final String END_BEFORE_START = "execution.period.endBeforeStart";
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
        Duration elapsed = Duration.between(startedAt, endedAt);
        if (elapsed.isNegative()) {
            throw new DomainRuleException("endedAt", END_BEFORE_START);
        }
        return ofElapsed(elapsed);
    }

    static int ofElapsed(Duration elapsed) {
        if (elapsed.isNegative()) {
            throw new DomainRuleException("endedAt", END_BEFORE_START);
        }
        long wholeMinutes = elapsed.toMinutes();
        boolean remainder = elapsed.minusMinutes(wholeMinutes).compareTo(Duration.ZERO) > 0;
        long minutes = wholeMinutes + (remainder ? 1 : 0);
        if (minutes > Integer.MAX_VALUE) {
            throw new DomainRuleException("endedAt", TOO_LONG);
        }
        return (int) minutes;
    }
}
