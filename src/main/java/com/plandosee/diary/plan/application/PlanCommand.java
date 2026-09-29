package com.plandosee.diary.plan.application;

import java.time.LocalDate;

import com.plandosee.diary.common.domain.FieldCodes;
import com.plandosee.diary.common.domain.InputCheck;
import com.plandosee.diary.common.domain.Priority;
import com.plandosee.diary.plan.domain.PlanPeriod;
import com.plandosee.diary.plan.domain.PlanRules;

/**
 * Plan content for create, revise and the improvement transfer. ADR-30: a self-validating command. The constructor
 * checks every field with the common tools and PlanRules (the same codes the form shows, ADR-22) and throws one
 * DomainRuleException listing every broken rule, so an invalid command cannot exist and no service has to remember a
 * check. Text is stored normalized (LF line breaks, stripped).
 */
public record PlanCommand(
        String title,
        LocalDate startDate,
        LocalDate endDate,
        Priority priority,
        String successCriteria,
        int estimatedMinutes) {

    public PlanCommand {
        InputCheck check = new InputCheck();
        title = check.text("title", PlanRules.TITLE, title);
        check.date("startDate", startDate, FieldCodes.START_DATE_REQUIRED);
        check.date("endDate", endDate, FieldCodes.END_DATE_REQUIRED);
        PlanPeriod.check(check, startDate, endDate);
        check.required("priority", priority, FieldCodes.PRIORITY_REQUIRED);
        successCriteria = check.text("successCriteria", PlanRules.SUCCESS_CRITERIA, successCriteria);
        check.range("estimatedMinutes", estimatedMinutes, PlanRules.ESTIMATED_MINUTES);
        check.done();
    }
}
