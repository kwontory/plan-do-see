package com.plandosee.diary.common.web;

import org.springframework.beans.BeanWrapperImpl;
import org.springframework.validation.BindingResult;

import com.plandosee.diary.common.error.DomainRuleException;

/**
 * Shows a domain rule violation on the form that caused it instead of a generic 400 page.
 * The error code is registered without a default message so the template resolves it through messages.properties.
 * The field error is attached when the form has that property; otherwise it becomes a global form error.
 */
public final class FormErrors {

    private FormErrors() {
    }

    public static void reject(BindingResult result, DomainRuleException ex) {
        String field = ex.field();
        Object target = result.getTarget();
        Object[] args = ex.args();
        if (field != null && target != null && new BeanWrapperImpl(target).isReadableProperty(field)) {
            result.rejectValue(field, ex.code(), args.length == 0 ? null : args, null);
        } else {
            result.reject(ex.code(), args.length == 0 ? null : args, null);
        }
    }
}
