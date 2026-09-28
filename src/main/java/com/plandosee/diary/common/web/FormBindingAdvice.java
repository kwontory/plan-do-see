package com.plandosee.diary.common.web;

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
 * Conversion failures (for example letters in a minutes field) become a plain Korean field error
 * instead of the framework message, which would expose Java type names.
 */
@ControllerAdvice
public class FormBindingAdvice {

    public static final String TYPE_MISMATCH_MESSAGE = "입력 형식이 올바르지 않습니다.";

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
            String[] codes = bindingResult.resolveMessageCodes(ex.getErrorCode(), field);
            bindingResult.addError(new FieldError(bindingResult.getObjectName(), field, ex.getValue(), true,
                    codes, null, TYPE_MISMATCH_MESSAGE));
        }
    }
}
