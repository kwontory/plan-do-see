package com.plandosee.diary.review.domain;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Evidence lists narrow the same ReviewScope by one metric. It is built only from a ReviewScope so the
 * summary and its drill-down cannot drift apart.
 */
public record EvidenceQuery(UUID userId, UUID planId, LocalDate today, String metricKey) {

    public static EvidenceQuery of(ReviewScope scope, ReviewMetric metric) {
        return new EvidenceQuery(scope.userId(), scope.planId(), scope.today(), metric.key());
    }
}
