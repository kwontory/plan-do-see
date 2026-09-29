package com.plandosee.diary.common.web;

import org.springframework.beans.BeanWrapperImpl;
import org.springframework.validation.BindingResult;

import com.plandosee.diary.common.error.DomainRuleException;
import com.plandosee.diary.common.error.FieldViolation;

/**
 * The one path from a rule violation to the form that caused it, instead of a generic 400 page:
 * every violation of the exception (a command check reports all broken fields at once; a DB constraint reports one)
 * becomes a field error when the form has that property, otherwise a global form error. A field that already has an
 * error from Bean Validation or binding keeps it and gets no second one.
 * The template resolves the error code through messages.properties. The code is also passed as the default message
 * only so that a code whose text has not been written yet cannot break the page (NoSuchMessageException, a 500);
 * MessageCatalogTest reports such a missing text (same approach as ConflictResponses).
 */
public final class FormErrors {

    private FormErrors() {
    }

    public static void reject(BindingResult result, DomainRuleException ex) {
        Object target = result.getTarget();
        BeanWrapperImpl bean = target == null ? null : new BeanWrapperImpl(target);
        for (FieldViolation violation : ex.violations()) {
            String field = violation.field();
            Object[] args = violation.args();
            Object[] messageArgs = args.length == 0 ? null : args;
            if (field != null && bean != null && bean.isReadableProperty(field)) {
                if (!result.hasFieldErrors(field)) {
                    result.rejectValue(field, violation.code(), messageArgs, violation.code());
                }
            } else {
                result.reject(violation.code(), messageArgs, violation.code());
            }
        }
    }
}
