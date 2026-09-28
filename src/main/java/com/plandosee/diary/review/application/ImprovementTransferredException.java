package com.plandosee.diary.review.application;

import com.plandosee.diary.common.error.DomainRuleException;

/**
 * ADR-08 / ADR-18 E10: the improvement was already carried to a next plan, so it cannot be edited. A rule
 * violation with the code {@link ReviewService#IMPROVEMENT_ALREADY_TRANSFERRED}; the web layer answers 409 and
 * shows the transferred improvement and the next plan with the user's input kept.
 */
public class ImprovementTransferredException extends DomainRuleException {

    public ImprovementTransferredException() {
        super("improvement", ReviewService.IMPROVEMENT_ALREADY_TRANSFERRED);
    }
}
