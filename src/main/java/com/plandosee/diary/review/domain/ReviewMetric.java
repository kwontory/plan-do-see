package com.plandosee.diary.review.domain;

import java.util.Optional;

/**
 * Review metrics and their drill-down keys (web-contract.md). The key is the only value accepted from the URL.
 * Labels and units live in messages.properties as metric.&lt;key&gt;.label / metric.&lt;key&gt;.unit, and the
 * card order is decided by the template (ADR-13).
 */
public enum ReviewMetric {
    PLANNED("planned"),
    COMPLETED("completed"),
    OVERDUE("overdue"),
    BLOCKED("blocked"),
    ESTIMATED("estimated"),
    ACTUAL("actual"),
    VARIANCE("variance");

    private final String key;

    ReviewMetric(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public long valueOf(ReviewSummary summary) {
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
