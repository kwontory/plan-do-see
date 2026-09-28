package com.plandosee.diary.plan.application;

import java.util.List;

import org.springframework.stereotype.Component;

import com.plandosee.diary.common.domain.FieldCodes;
import com.plandosee.diary.common.error.ConstraintCode;
import com.plandosee.diary.common.error.ConstraintCodeSource;
import com.plandosee.diary.common.error.ConstraintViolationTranslator;
import com.plandosee.diary.plan.domain.PlanPeriod;

/**
 * ADR-22: plan table constraints (V1) and the code each means. PlanService checks the same rules first (PlanRules),
 * so these are reached only if a rule and its CHECK ever drift apart; a blank title is caught before the DB, so
 * ck_plans_title can only mean "too long".
 */
@Component
public class PlanConstraintCodes implements ConstraintCodeSource {

    @Override
    public List<ConstraintCode> constraintCodes() {
        return List.of(
                new ConstraintCode("ck_plans_title", "title", FieldCodes.TITLE_MAX),
                new ConstraintCode("ck_plans_period", "endDate", PlanPeriod.END_BEFORE_START),
                new ConstraintCode("ck_plans_priority", "priority", FieldCodes.PRIORITY_REQUIRED),
                new ConstraintCode("ck_plans_success_criteria", "successCriteria", FieldCodes.SUCCESS_CRITERIA_MAX),
                new ConstraintCode("ck_plans_estimated_minutes", "estimatedMinutes",
                        ConstraintViolationTranslator.VALUE_INVALID));
    }
}
