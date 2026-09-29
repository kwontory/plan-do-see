package com.plandosee.diary.review.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Evidence lists narrow the same ReviewScope by one metric. It is built only from a ReviewScope so the
 * summary and its drill-down cannot drift apart. limit/offset select one page (null: the whole list);
 * todoIds (null: no narrowing) limits log reads to given todos, used to attach blocker reasons to a page of todos.
 */
public record EvidenceQuery(UUID userId, UUID planId, LocalDate today, String metricKey, Integer limit, Long offset,
                            List<UUID> todoIds) {

    public EvidenceQuery {
        todoIds = todoIds == null ? null : List.copyOf(todoIds);
    }

    public static EvidenceQuery of(ReviewScope scope, ReviewMetric metric) {
        return new EvidenceQuery(scope.userId(), scope.planId(), scope.today(), metric.key(), null, null, null);
    }

    public EvidenceQuery page(int pageLimit, long pageOffset) {
        return new EvidenceQuery(userId, planId, today, metricKey, pageLimit, pageOffset, todoIds);
    }

    public EvidenceQuery forTodos(List<UUID> ids) {
        return new EvidenceQuery(userId, planId, today, metricKey, null, null, ids);
    }
}
