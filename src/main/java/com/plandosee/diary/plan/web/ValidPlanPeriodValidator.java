package com.plandosee.diary.plan.web;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.plandosee.diary.plan.domain.PlanPeriod;

public class ValidPlanPeriodValidator implements ConstraintValidator<ValidPlanPeriod, PlanForm> {

    @Override
    public boolean isValid(PlanForm form, ConstraintValidatorContext context) {
        if (form == null || PlanPeriod.isValid(form.getStartDate(), form.getEndDate())) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("endDate")
                .addConstraintViolation();
        return false;
    }
}
