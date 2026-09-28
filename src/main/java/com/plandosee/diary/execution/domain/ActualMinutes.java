package com.plandosee.diary.execution.domain;

import java.time.Duration;
import java.time.OffsetDateTime;

import com.plandosee.diary.common.error.DomainRuleException;

/**
 * DEC-03: actual minutes are computed by the server from the elapsed time and rounded up so work is never
 * under-counted. Exactly zero elapsed time is 0 minutes; 1..60 seconds is 1 minute; 61 seconds is 2 minutes.
 */
public final class ActualMinutes {

    private ActualMinutes() {
    }

    public static int between(OffsetDateTime startedAt, OffsetDateTime endedAt) {
        if (startedAt == null) {
            throw new DomainRuleException("startedAt", "시작 시각을 입력하세요.");
        }
        if (endedAt == null) {
            throw new DomainRuleException("endedAt", "종료 시각을 입력하세요.");
        }
        Duration elapsed = Duration.between(startedAt, endedAt);
        if (elapsed.isNegative()) {
            throw new DomainRuleException("endedAt", "종료 시각은 시작 시각보다 빠를 수 없습니다.");
        }
        return ofElapsed(elapsed);
    }

    static int ofElapsed(Duration elapsed) {
        if (elapsed.isNegative()) {
            throw new DomainRuleException("endedAt", "종료 시각은 시작 시각보다 빠를 수 없습니다.");
        }
        long wholeMinutes = elapsed.toMinutes();
        boolean remainder = elapsed.minusMinutes(wholeMinutes).compareTo(Duration.ZERO) > 0;
        long minutes = wholeMinutes + (remainder ? 1 : 0);
        if (minutes > Integer.MAX_VALUE) {
            throw new DomainRuleException("endedAt", "실행 기간이 너무 깁니다. 시작·종료 시각의 연도와 날짜를 확인하세요.");
        }
        return (int) minutes;
    }
}
