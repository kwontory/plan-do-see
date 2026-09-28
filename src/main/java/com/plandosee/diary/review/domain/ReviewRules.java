package com.plandosee.diary.review.domain;

import com.plandosee.diary.common.domain.FieldCodes;
import com.plandosee.diary.common.domain.FieldRules;

/**
 * ADR-22: the one place for the review field rules. ImprovementForm's annotation and ReviewService's entry check use
 * this value. reviews.improvement is TEXT without a length CHECK in V1, so the service is the last line here.
 */
public final class ReviewRules {

    public static final int IMPROVEMENT_MAX = 1000;

    private ReviewRules() {
    }

    /** Optional improvement: only the length after strip is checked (blank clears it). */
    public static void checkImprovement(String improvement) {
        FieldRules.maxText("improvement", improvement, IMPROVEMENT_MAX, FieldCodes.IMPROVEMENT_MAX);
    }
}
