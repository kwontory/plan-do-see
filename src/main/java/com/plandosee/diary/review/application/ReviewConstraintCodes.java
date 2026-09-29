package com.plandosee.diary.review.application;

import java.util.List;

import org.springframework.stereotype.Component;

import com.plandosee.diary.common.domain.FieldCodes;
import com.plandosee.diary.common.error.ConstraintCode;
import com.plandosee.diary.common.error.ConstraintCodeSource;

/**
 * Review table constraints (V5) and the code each means. ReviewService checks the same rule first
 * (ReviewRules.IMPROVEMENT).
 */
@Component
public class ReviewConstraintCodes implements ConstraintCodeSource {

    @Override
    public List<ConstraintCode> constraintCodes() {
        return List.of(new ConstraintCode("ck_reviews_improvement_max", "improvement", FieldCodes.IMPROVEMENT_MAX));
    }
}
