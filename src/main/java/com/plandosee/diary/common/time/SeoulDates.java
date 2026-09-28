package com.plandosee.diary.common.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

/**
 * "Today" is always judged in Asia/Seoul, independent of the JVM or DB session time zone.
 */
public class SeoulDates {

    private final Clock clock;

    public SeoulDates(Clock clock) {
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock.withZone(TimeConfig.SEOUL));
    }

    public Instant now() {
        return clock.instant();
    }
}
