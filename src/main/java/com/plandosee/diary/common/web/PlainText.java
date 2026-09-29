package com.plandosee.diary.common.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import com.plandosee.diary.common.domain.TextInput;

/**
 * ADR-30 form rule for the content of a text field: the common rule {@link TextInput#contentCode} (a single-line box
 * without line breaks or control characters; a multi-line box with LF and TAB only). The message is the broken rule's
 * code ({@code {validation.text.lineBreak}} or {@code {validation.text.controlChar}}). Required and length stay on
 * {@code @NotBlank}/{@code @Size} with the rule constants; the value must match the field's TextRule
 * (ValidationRulesConsistencyTest). The command check applies the same rule again (ADR-22).
 */
@Documented
@Constraint(validatedBy = PlainTextValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PlainText {

    TextInput.Lines value();

    String message() default "{validation.text.controlChar}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
