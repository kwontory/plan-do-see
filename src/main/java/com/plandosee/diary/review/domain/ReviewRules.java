package com.plandosee.diary.review.domain;

import com.plandosee.diary.common.domain.FieldCodes;
import com.plandosee.diary.common.domain.TextInput;
import com.plandosee.diary.common.domain.TextRule;

/**
 * ADR-22 / ADR-30: the one place for the review field rules. ImprovementForm's annotations and ReviewService (through
 * {@link #IMPROVEMENT}) use this value, and it equals the V5 CHECK ck_reviews_improvement_max (and
 * ck_plans_carried_improvement_max for the transferred copy); ValidationRulesConsistencyTest compares them.
 */
public final class ReviewRules {

    public static final int IMPROVEMENT_MAX = 1000;

    /** Optional, several lines; blank or invisible-only text clears it. */
    public static final TextRule IMPROVEMENT =
            TextRule.optional(TextInput.Lines.MULTI, IMPROVEMENT_MAX, FieldCodes.IMPROVEMENT_MAX);

    private ReviewRules() {
    }
}
