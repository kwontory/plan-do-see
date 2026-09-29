package com.plandosee.diary.review.web;

import jakarta.validation.constraints.Size;

import com.plandosee.diary.common.domain.TextInput;
import com.plandosee.diary.common.web.PlainText;
import com.plandosee.diary.review.domain.ReviewRules;

/**
 * Optional improvement (1..ReviewRules.IMPROVEMENT_MAX chars after trim; blank clears it). Read-only after transfer.
 */
public class ImprovementForm {

    @Size(max = ReviewRules.IMPROVEMENT_MAX, message = "{validation.improvement.max}")
    @PlainText(TextInput.Lines.MULTI)
    private String improvement;

    private Integer version;

    public static ImprovementForm of(String improvement) {
        ImprovementForm form = new ImprovementForm();
        form.setImprovement(improvement);
        return form;
    }

    /** The stored improvement and the version to send back. */
    public static ImprovementForm of(com.plandosee.diary.review.domain.ReviewRow review) {
        ImprovementForm form = of(review.getImprovement());
        form.setVersion(review.getVersion());
        return form;
    }

    public String getImprovement() {
        return improvement;
    }

    public void setImprovement(String improvement) {
        this.improvement = improvement;
    }

    /**
     * Hidden field: the version the edit form was opened with (after a stale-version conflict, the latest
     * version). Only compared to detect an out-of-date save; never an authorization value. Absent: no check.
     */
    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
