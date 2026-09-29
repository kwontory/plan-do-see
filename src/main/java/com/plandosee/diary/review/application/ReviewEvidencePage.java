package com.plandosee.diary.review.application;

import com.plandosee.diary.plan.domain.PlanRow;
import com.plandosee.diary.review.domain.ReviewEvidence;
import com.plandosee.diary.review.domain.ReviewRow;

/**
 * Everything the evidence page shows, read in one repeatable-read snapshot.
 */
public record ReviewEvidencePage(ReviewRow review, PlanRow plan, ReviewEvidence evidence) {
}
