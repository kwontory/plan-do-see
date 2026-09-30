package com.plandosee.diary.transfer.domain;

/**
 * Row counts of one owner in every table that holds owned data, soft-deleted rows included (plans, tags and reviews
 * by user_id; the others through their plan or todo).
 */
public class OwnedCounts {

    private long plans;
    private long planRevisions;
    private long todos;
    private long todoRevisions;
    private long todoTags;
    private long tags;
    private long executionLogs;
    private long completionEvents;
    private long reopenEvents;
    private long reviews;

    public long total() {
        return plans + planRevisions + todos + todoRevisions + todoTags + tags + executionLogs + completionEvents
                + reopenEvents + reviews;
    }

    /** Rows whose owner column is changed by the transfer (children follow their plan or todo). */
    public long ownerRows() {
        return plans + tags + reviews;
    }

    /** key=value pairs for the log line (table counts only, never data). */
    public String describe() {
        return "plans=" + plans + " planRevisions=" + planRevisions + " todos=" + todos + " todoRevisions="
                + todoRevisions + " todoTags=" + todoTags + " tags=" + tags + " executionLogs=" + executionLogs
                + " completionEvents=" + completionEvents + " reopenEvents=" + reopenEvents + " reviews=" + reviews;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof OwnedCounts o && describe().equals(o.describe());
    }

    @Override
    public int hashCode() {
        return describe().hashCode();
    }

    @Override
    public String toString() {
        return describe();
    }

    public long getPlans() { return plans; }
    public void setPlans(long plans) { this.plans = plans; }
    public long getPlanRevisions() { return planRevisions; }
    public void setPlanRevisions(long planRevisions) { this.planRevisions = planRevisions; }
    public long getTodos() { return todos; }
    public void setTodos(long todos) { this.todos = todos; }
    public long getTodoRevisions() { return todoRevisions; }
    public void setTodoRevisions(long todoRevisions) { this.todoRevisions = todoRevisions; }
    public long getTodoTags() { return todoTags; }
    public void setTodoTags(long todoTags) { this.todoTags = todoTags; }
    public long getTags() { return tags; }
    public void setTags(long tags) { this.tags = tags; }
    public long getExecutionLogs() { return executionLogs; }
    public void setExecutionLogs(long executionLogs) { this.executionLogs = executionLogs; }
    public long getCompletionEvents() { return completionEvents; }
    public void setCompletionEvents(long completionEvents) { this.completionEvents = completionEvents; }
    public long getReopenEvents() { return reopenEvents; }
    public void setReopenEvents(long reopenEvents) { this.reopenEvents = reopenEvents; }
    public long getReviews() { return reviews; }
    public void setReviews(long reviews) { this.reviews = reviews; }
}
