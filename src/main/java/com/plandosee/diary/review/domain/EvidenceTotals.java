package com.plandosee.diary.review.domain;

/**
 * Row count and minute sums of one metric's whole evidence list (not a page), read with the list's own filter.
 * estimatedMinutes is 0 for log lists.
 */
public class EvidenceTotals {

    private long count;
    private long estimatedMinutes;
    private long actualMinutes;

    public static EvidenceTotals zero() {
        return new EvidenceTotals();
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public long getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(long estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    public long getActualMinutes() {
        return actualMinutes;
    }

    public void setActualMinutes(long actualMinutes) {
        this.actualMinutes = actualMinutes;
    }
}
