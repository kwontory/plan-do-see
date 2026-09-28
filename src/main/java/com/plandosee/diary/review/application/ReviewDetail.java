package com.plandosee.diary.review.application;

import com.plandosee.diary.plan.domain.PlanRow;
import com.plandosee.diary.review.domain.ReviewRow;
import com.plandosee.diary.review.domain.ReviewSummary;

/**
 * Everything the S04 review page shows, read in one snapshot (ADR-14 C-4). nextPlan is null until the
 * improvement is transferred.
 */
public record ReviewDetail(ReviewRow review, PlanRow plan, PlanRow nextPlan, ReviewSummary summary) {
}
