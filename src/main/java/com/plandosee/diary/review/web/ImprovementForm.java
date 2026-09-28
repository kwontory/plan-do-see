package com.plandosee.diary.review.web;

import jakarta.validation.constraints.Size;

/**
 * Optional improvement (1..1000 chars after trim; blank clears it). Read-only after transfer (ADR-08).
 */
public class ImprovementForm {

    @Size(max = 1000, message = "{validation.improvement.max}")
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
