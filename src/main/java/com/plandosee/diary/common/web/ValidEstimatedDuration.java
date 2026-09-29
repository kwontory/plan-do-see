package com.plandosee.diary.common.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Form-level rule for the three estimated-time boxes (like {@code @ValidPlanPeriod}). Reports one
 * violation on the field {@value EstimatedDurationForm#FIELD} whose message template is the broken rule's code
 * ({@code {validation.estimatedMinutes.*}}), in the same validation pass as the other fields.
 */
@Documented
@Constraint(validatedBy = ValidEstimatedDurationValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEstimatedDuration {

    String message() default "{validation.estimatedMinutes.required}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
