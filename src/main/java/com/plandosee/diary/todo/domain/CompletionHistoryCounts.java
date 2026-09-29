package com.plandosee.diary.todo.domain;

/**
 * Numbers of completion and reopen events of one todo (all of them, not a page).
 */
public class CompletionHistoryCounts {

    private long completionCount;
    private long reopenCount;

    public long getCompletionCount() {
        return completionCount;
    }

    public void setCompletionCount(long completionCount) {
        this.completionCount = completionCount;
    }

    public long getReopenCount() {
        return reopenCount;
    }

    public void setReopenCount(long reopenCount) {
        this.reopenCount = reopenCount;
    }

    /** Rows of the merged history. */
    public long total() {
        return completionCount + reopenCount;
    }
}
