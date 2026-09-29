package com.plandosee.diary.common.web;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.plandosee.diary.common.domain.TextInput;

public class PlainTextValidator implements ConstraintValidator<PlainText, String> {

    private TextInput.Lines lines;

    @Override
    public void initialize(PlainText annotation) {
        this.lines = annotation.value();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        String code = TextInput.contentCode(lines, TextInput.normalize(value));
        if (code == null) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("{" + code + "}").addConstraintViolation();
        return false;
    }
}
