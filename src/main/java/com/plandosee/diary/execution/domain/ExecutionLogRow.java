package com.plandosee.diary.execution.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.plandosee.diary.common.domain.DurationParts;

/**
 * An execution record. Start, end (with the recomputed actual minutes) and the blocker reason can be edited; the
 * previous values go to execution_log_revisions (ADR-40). todoTitle is a joined display column and is never written.
 * version counts saved edits (edit-conflict detection, ADR-18).
 */
public class ExecutionLogRow {

    private UUID id;
    private UUID todoId;
    private String todoTitle;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private int actualMinutes;
    private String blockerReason;
    private OffsetDateTime createdAt;
    private int version;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTodoId() {
        return todoId;
    }

    public void setTodoId(UUID todoId) {
        this.todoId = todoId;
    }

    public String getTodoTitle() {
        return todoTitle;
    }

    public void setTodoTitle(String todoTitle) {
        this.todoTitle = todoTitle;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(OffsetDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public int getActualMinutes() {
        return actualMinutes;
    }

    public void setActualMinutes(int actualMinutes) {
        this.actualMinutes = actualMinutes;
    }

    /** actualMinutes split into days, hours and minutes for display (not stored, not exported). */
    public DurationParts getActualDuration() {
        return DurationParts.of(actualMinutes);
    }

    public String getBlockerReason() {
        return blockerReason;
    }

    public void setBlockerReason(String blockerReason) {
        this.blockerReason = blockerReason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
