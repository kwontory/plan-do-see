package com.plandosee.diary.review.domain;

/**
 * Raw result row of the summary query. Minute sums are long (SUM over many todos can exceed int).
 * Converted to the immutable ReviewSummary by the service.
 */
public class ReviewCounts {

    private int plannedCount;
    private int completedCount;
    private int overdueCount;
    private int blockedCount;
    private long estimatedMinutes;
    private long actualMinutes;
    private long varianceMinutes;

    public int getPlannedCount() {
        return plannedCount;
    }

    public void setPlannedCount(int plannedCount) {
        this.plannedCount = plannedCount;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(int completedCount) {
        this.completedCount = completedCount;
    }

    public int getOverdueCount() {
        return overdueCount;
    }

    public void setOverdueCount(int overdueCount) {
        this.overdueCount = overdueCount;
    }

    public int getBlockedCount() {
        return blockedCount;
    }

    public void setBlockedCount(int blockedCount) {
        this.blockedCount = blockedCount;
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

    public long getVarianceMinutes() {
        return varianceMinutes;
    }

    public void setVarianceMinutes(long varianceMinutes) {
        this.varianceMinutes = varianceMinutes;
    }
}
