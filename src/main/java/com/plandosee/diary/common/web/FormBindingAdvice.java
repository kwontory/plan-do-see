package com.plandosee.diary.common.web;

import java.beans.PropertyEditorSupport;
import java.util.Arrays;

import org.springframework.beans.PropertyAccessException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DefaultBindingErrorProcessor;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * Normalizes every submitted string: line breaks (CRLF, lone CR) become LF, then the value is trimmed and blank
 * input becomes null so @NotBlank and optional-field rules apply uniformly. Browsers count a textarea line break as
 * one character for maxlength but submit it as CRLF; normalizing before validation makes the form, the service check
 * and the DB char_length count the same characters, and stored text always uses LF (ADR-22, QA3-D1).
 * Conversion failures (for example letters in a minutes field) become a field error with the message key
 * {@link #TYPE_MISMATCH_CODE} and no default message, instead of the framework message that would expose
 * Java type names.
 */
@ControllerAdvice
public class FormBindingAdvice {

    public static final String TYPE_MISMATCH_CODE = "validation.typeMismatch";

    @InitBinder
    public void trimStrings(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new SubmittedTextEditor());
        binder.setBindingErrorProcessor(new FriendlyBindingErrorProcessor());
    }

    /** CRLF and lone CR to LF; null for null input. */
    public static String normalizeLineBreaks(String value) {
        if (value == null || value.indexOf('\r') < 0) {
            return value;
        }
        return value.replace("\r\n", "\n").replace('\r', '\n');
    }

    static final class SubmittedTextEditor extends PropertyEditorSupport {

        @Override
        public void setAsText(String text) {
            if (text == null) {
                setValue(null);
                return;
            }
            String value = normalizeLineBreaks(text).trim();
            setValue(value.isEmpty() ? null : value);
        }

        @Override
        public String getAsText() {
            Object value = getValue();
            return value == null ? "" : value.toString();
        }
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
