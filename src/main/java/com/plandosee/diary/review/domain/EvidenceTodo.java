package com.plandosee.diary.review.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.plandosee.diary.common.domain.Priority;
import com.plandosee.diary.todo.domain.TodoStatus;

/**
 * One todo in a review evidence list. actualMinutes is that todo's execution total, computed before any join.
 */
public class EvidenceTodo {

    private UUID id;
    private String title;
    private TodoStatus status;
    private LocalDate dueDate;
    private Priority priority;
    private int estimatedMinutes;
    private int actualMinutes;
    private boolean blocked;
    private List<String> blockerReasons = new ArrayList<>();
    private boolean overdue;
    private boolean dueToday;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public TodoStatus getStatus() {
        return status;
    }

    public void setStatus(TodoStatus status) {
        this.status = status;
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

    public int getActualMinutes() {
        return actualMinutes;
    }

    public void setActualMinutes(int actualMinutes) {
        this.actualMinutes = actualMinutes;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public List<String> getBlockerReasons() {
        return blockerReasons;
    }

    public void setBlockerReasons(List<String> blockerReasons) {
        this.blockerReasons = blockerReasons;
    }

    /** Status is COMPLETED. */
    public boolean isCompleted() {
        return status == TodoStatus.COMPLETED;
    }

    /** Same meaning as TodoRow.isOverdue: the review scope's is_overdue (TodoPredicates.overdue, T06-C30). */
    public boolean isOverdue() {
        return overdue;
    }

    public void setOverdue(boolean overdue) {
        this.overdue = overdue;
    }

    /** Same meaning as TodoRow.isDueToday (TodoPredicates.dueToday). */
    public boolean isDueToday() {
        return dueToday;
    }

    public void setDueToday(boolean dueToday) {
        this.dueToday = dueToday;
    }
}
