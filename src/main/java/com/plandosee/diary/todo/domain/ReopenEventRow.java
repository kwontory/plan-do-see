package com.plandosee.diary.todo.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * ADR-16: a completed todo put back to IN_PROGRESS. cycleNo is the completion cycle that was undone. Immutable.
 */
public class ReopenEventRow {

    private UUID id;
    private UUID todoId;
    private int cycleNo;
    private OffsetDateTime reopenedAt;
    private OffsetDateTime createdAt;

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

    public int getCycleNo() {
        return cycleNo;
    }

    public void setCycleNo(int cycleNo) {
        this.cycleNo = cycleNo;
    }

    public OffsetDateTime getReopenedAt() {
        return reopenedAt;
    }

    public void setReopenedAt(OffsetDateTime reopenedAt) {
        this.reopenedAt = reopenedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
