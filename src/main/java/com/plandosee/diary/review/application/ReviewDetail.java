package com.plandosee.diary.review.application;

import com.plandosee.diary.plan.domain.PlanRow;
import com.plandosee.diary.review.domain.ReviewRow;
import com.plandosee.diary.review.domain.ReviewSummary;

/**
 * Everything the review page shows, read in one snapshot. nextPlan is null until the
 * improvement is transferred.
 */
public record ReviewDetail(ReviewRow review, PlanRow plan, PlanRow nextPlan, ReviewSummary summary) {
}
