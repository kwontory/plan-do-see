package com.plandosee.diary.execution.application;

import java.time.OffsetDateTime;

import com.plandosee.diary.common.domain.InputCheck;
import com.plandosee.diary.common.time.TimeConfig;
import com.plandosee.diary.execution.domain.ActualMinutes;
import com.plandosee.diary.execution.domain.ExecutionRules;

/**
 * One execution record to store. ADR-30: a self-validating command. The constructor checks both times (required,
 * Seoul local date within DateBounds), the period (ActualMinutes: not reversed, at most
 * ExecutionRules.PERIOD_MAX_MINUTES) and the blocker reason (ExecutionRules.BLOCKER_REASON) and throws one
 * DomainRuleException listing every broken rule. blockerReason is stored normalized, null when blank.
 */
public record ExecutionCommand(OffsetDateTime startedAt, OffsetDateTime endedAt, String blockerReason) {

    public ExecutionCommand {
        InputCheck check = new InputCheck();
        check.dateTime("startedAt", startedAt, TimeConfig.SEOUL, ActualMinutes.STARTED_AT_REQUIRED);
        check.dateTime("endedAt", endedAt, TimeConfig.SEOUL, ActualMinutes.ENDED_AT_REQUIRED);
        if (check.ok("startedAt") && check.ok("endedAt")) {
            ActualMinutes.check(check, startedAt, endedAt);
        }
        blockerReason = check.text("blockerReason", ExecutionRules.BLOCKER_REASON, blockerReason);
        check.done();
    }

    /** DEC-03 whole minutes, rounded up (valid by construction). */
    public int actualMinutes() {
        return ActualMinutes.between(startedAt, endedAt);
    }
}
