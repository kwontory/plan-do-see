package com.plandosee.diary.review.domain;

/**
 * Raw result row of the summary query. Converted to the immutable ReviewSummary by the service.
 */
public class ReviewCounts {

    private int plannedCount;
    private int completedCount;
    private int overdueCount;
    private int blockedCount;
    private int estimatedMinutes;
    private int actualMinutes;
    private int varianceMinutes;

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

    public int getVarianceMinutes() {
        return varianceMinutes;
    }

    public void setVarianceMinutes(int varianceMinutes) {
        this.varianceMinutes = varianceMinutes;
    }
}
