package com.plandosee.diary.common.web;

import java.util.Arrays;

import org.springframework.beans.PropertyAccessException;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DefaultBindingErrorProcessor;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * Trims every submitted string; blank input becomes null so @NotBlank and optional-field rules apply uniformly.
 * Conversion failures (for example letters in a minutes field) become a field error with the message key
 * {@link #TYPE_MISMATCH_CODE} and no default message, instead of the framework message that would expose
 * Java type names.
 */
@ControllerAdvice
public class FormBindingAdvice {

    public static final String TYPE_MISMATCH_CODE = "validation.typeMismatch";

    @InitBinder
    public void trimStrings(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
        binder.setBindingErrorProcessor(new FriendlyBindingErrorProcessor());
    }

    static final class FriendlyBindingErrorProcessor extends DefaultBindingErrorProcessor {

        @Override
        public void processPropertyAccessException(PropertyAccessException ex, BindingResult bindingResult) {
            String field = ex.getPropertyName();
            if (field == null) {
                super.processPropertyAccessException(ex, bindingResult);
                return;
            }
            // Framework codes first (a field-specific override may exist); the generic key is the last resort.
            String[] resolved = bindingResult.resolveMessageCodes(ex.getErrorCode(), field);
            String[] codes = Arrays.copyOf(resolved, resolved.length + 1);
            codes[resolved.length] = TYPE_MISMATCH_CODE;
            bindingResult.addError(new FieldError(bindingResult.getObjectName(), field, ex.getValue(), true,
                    codes, null, null));
        }
    }
}
