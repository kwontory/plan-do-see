package com.plandosee.diary.common.web;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.plandosee.diary.common.domain.DurationInput;

public class ValidEstimatedDurationValidator implements ConstraintValidator<ValidEstimatedDuration, EstimatedDurationForm> {

    @Override
    public boolean isValid(EstimatedDurationForm form, ConstraintValidatorContext context) {
        if (form == null) {
            return true;
        }
        DurationInput.Result result = form.estimatedInput();
        if (result.valid()) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("{" + result.code() + "}")
                .addPropertyNode(EstimatedDurationForm.FIELD)
                .addConstraintViolation();
        return false;
    }
}
