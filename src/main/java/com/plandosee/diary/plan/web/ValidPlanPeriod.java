package com.plandosee.diary.plan.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Form-level plan period rule. Reuses {@link com.plandosee.diary.plan.domain.PlanPeriod} and reports
 * the violation on the {@code endDate} field, in the same validation pass as every other field.
 * The message is the rule's error code as a message key.
 */
@Documented
@Constraint(validatedBy = ValidPlanPeriodValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPlanPeriod {

    String message() default "{plan.period.endBeforeStart}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
