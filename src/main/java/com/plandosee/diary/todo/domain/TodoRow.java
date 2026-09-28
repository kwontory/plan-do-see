package com.plandosee.diary.todo.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.plandosee.diary.common.domain.Priority;

public class TodoRow {

    private UUID id;
    private UUID planId;
    private String title;
    private LocalDate dueDate;
    private Priority priority;
    private int estimatedMinutes;
    private TodoStatus status;
    private int completionCycle;
    private OffsetDateTime completedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private int version;
    private List<TagRow> tags = new ArrayList<>();
    private boolean overdue;
    private boolean dueToday;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPlanId() {
        return planId;
    }

    public void setPlanId(UUID planId) {
        this.planId = planId;
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

    public TodoStatus getStatus() {
        return status;
    }

    public void setStatus(TodoStatus status) {
        this.status = status;
    }

    public int getCompletionCycle() {
        return completionCycle;
    }

    public void setCompletionCycle(int completionCycle) {
        this.completionCycle = completionCycle;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<TagRow> getTags() {
        return tags;
    }

    public void setTags(List<TagRow> tags) {
        this.tags = tags;
    }

    /** Status is COMPLETED. */
    public boolean isCompleted() {
        return status == TodoStatus.COMPLETED;
    }

    /**
     * Overdue in Asia/Seoul: not completed and due before today (TodoPredicates.overdue, T06-C30). Set by display
     * reads (get, search); false on rows read for a write.
     */
    public boolean isOverdue() {
        return overdue;
    }

    public void setOverdue(boolean overdue) {
        this.overdue = overdue;
    }

    /**
     * Due date equals today in Asia/Seoul, whatever the status (TodoPredicates.dueToday, same as the DUE_TODAY
     * filter). Set by display reads (get, search); false on rows read for a write.
     */
    public boolean isDueToday() {
        return dueToday;
    }

    public void setDueToday(boolean dueToday) {
        this.dueToday = dueToday;
    }

    /** ADR-18 edit version: +1 only when an edit form changes the content (E5, E7). */
    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
