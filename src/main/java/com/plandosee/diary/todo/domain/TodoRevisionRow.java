package com.plandosee.diary.todo.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.plandosee.diary.common.domain.Priority;

/**
 * ADR-16: the values of a todo just before one successful edit. Immutable once stored. tagNames is the snapshot of
 * the tag display names at that moment, ordered by normalized name.
 */
public class TodoRevisionRow {

    private UUID id;
    private UUID todoId;
    private int revisionNo;
    private String title;
    private LocalDate dueDate;
    private Priority priority;
    private int estimatedMinutes;
    private List<String> tagNames = List.of();
    private OffsetDateTime revisedAt;

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

    public int getRevisionNo() {
        return revisionNo;
    }

    public void setRevisionNo(int revisionNo) {
        this.revisionNo = revisionNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(int estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    public List<String> getTagNames() {
        return tagNames;
    }

    public void setTagNames(List<String> tagNames) {
        this.tagNames = tagNames == null ? List.of() : List.copyOf(tagNames);
    }

    public OffsetDateTime getRevisedAt() {
        return revisedAt;
    }

    public void setRevisedAt(OffsetDateTime revisedAt) {
        this.revisedAt = revisedAt;
    }
}
