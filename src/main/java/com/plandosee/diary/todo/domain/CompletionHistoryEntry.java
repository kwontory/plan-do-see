package com.plandosee.diary.todo.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * ADR-16: one row of a todo's completion history, merging completion_events and reopen_events. cycleNo is the
 * completion cycle; for REOPENED it is the cycle that was undone. id is the event's own id (tie-breaker).
 */
public class CompletionHistoryEntry {

    private UUID id;
    private CompletionHistoryKind kind;
    private int cycleNo;
    private OffsetDateTime occurredAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public CompletionHistoryKind getKind() {
        return kind;
    }

    public void setKind(CompletionHistoryKind kind) {
        this.kind = kind;
    }

    public int getCycleNo() {
        return cycleNo;
    }

    public void setCycleNo(int cycleNo) {
        this.cycleNo = cycleNo;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(OffsetDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}
