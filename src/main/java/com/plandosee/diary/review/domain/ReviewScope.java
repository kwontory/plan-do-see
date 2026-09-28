package com.plandosee.diary.review.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * DEC-01: the single scope shared by the review summary and every evidence list. userId is server-resolved.
 * Property names match the parameters used by mapper/common/TodoPredicates.xml.
 */
public record ReviewScope(UUID userId, UUID planId, LocalDate today) {

    public ReviewScope {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(today, "today");
    }
}
