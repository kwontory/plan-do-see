package com.plandosee.diary.export.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ExportTodoTagRow {

    private UUID todoId;
    private UUID tagId;
    private OffsetDateTime createdAt;

    public UUID getTodoId() {
        return todoId;
    }

    public void setTodoId(UUID todoId) {
        this.todoId = todoId;
    }

    public UUID getTagId() {
        return tagId;
    }

    public void setTagId(UUID tagId) {
        this.tagId = tagId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
