package com.plandosee.diary.review.web;

import jakarta.validation.constraints.Size;

import com.plandosee.diary.review.domain.ReviewRules;

/**
 * Optional improvement (1..ReviewRules.IMPROVEMENT_MAX chars after trim; blank clears it). Read-only after transfer
 * (ADR-08).
 */
public class ImprovementForm {

    @Size(max = ReviewRules.IMPROVEMENT_MAX, message = "{validation.improvement.max}")
    private String improvement;

    public static ImprovementForm of(String improvement) {
        ImprovementForm form = new ImprovementForm();
        form.setImprovement(improvement);
        return form;
    }

    public String getImprovement() {
        return improvement;
    }

    public void setImprovement(String improvement) {
        this.improvement = improvement;
    }
}
