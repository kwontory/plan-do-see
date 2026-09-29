package com.plandosee.diary.review.domain;

import java.time.LocalDate;

/**
 * Review aggregate for one ReviewScope. Counts are int; minute sums are long. varianceMinutes = actualMinutes - estimatedMinutes. Empty scope is all zero.
 */
public record ReviewSummary(
        int plannedCount,
        int completedCount,
        int overdueCount,
        int blockedCount,
        long estimatedMinutes,
        long actualMinutes,
        long varianceMinutes,
        LocalDate today) {

    public static ReviewSummary of(ReviewCounts counts, LocalDate today) {
        return new ReviewSummary(
                counts.getPlannedCount(),
                counts.getCompletedCount(),
                counts.getOverdueCount(),
                counts.getBlockedCount(),
                counts.getEstimatedMinutes(),
                counts.getActualMinutes(),
                counts.getVarianceMinutes(),
                today);
    }

    /** ADR-27: estimated/actual bar widths derived from this summary's minute sums (not a record component). */
    public ReviewBars bars() {
        return ReviewBars.of(estimatedMinutes, actualMinutes);
    }
}
