package com.plandosee.diary.review.domain;

import java.util.Optional;

/**
 * Review metrics and their drill-down keys (web-contract.md). The key is the only value accepted from the URL.
 */
public enum ReviewMetric {
    PLANNED("planned", "계획 수(할 일 수)", "건"),
    COMPLETED("completed", "완료 수", "건"),
    OVERDUE("overdue", "지연 수", "건"),
    BLOCKED("blocked", "막힘 수", "건"),
    ESTIMATED("estimated", "예상 시간", "분"),
    ACTUAL("actual", "실제 시간", "분"),
    VARIANCE("variance", "차이(실제 - 예상)", "분");

    private final String key;
    private final String label;
    private final String unit;

    ReviewMetric(String key, String label, String unit) {
        this.key = key;
        this.label = label;
        this.unit = unit;
    }

    public String key() {
        return key;
    }

    public String label() {
        return label;
    }

    public String unit() {
        return unit;
    }

    public int valueOf(ReviewSummary summary) {
        return switch (this) {
            case PLANNED -> summary.plannedCount();
            case COMPLETED -> summary.completedCount();
            case OVERDUE -> summary.overdueCount();
            case BLOCKED -> summary.blockedCount();
            case ESTIMATED -> summary.estimatedMinutes();
            case ACTUAL -> summary.actualMinutes();
            case VARIANCE -> summary.varianceMinutes();
        };
    }

    public static Optional<ReviewMetric> fromKey(String key) {
        if (key != null) {
            for (ReviewMetric metric : values()) {
                if (metric.key.equals(key)) {
                    return Optional.of(metric);
                }
            }
        }
        return Optional.empty();
    }
}
