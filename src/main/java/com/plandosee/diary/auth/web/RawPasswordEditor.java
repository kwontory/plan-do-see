package com.plandosee.diary.auth.web;

import java.beans.PropertyEditorSupport;
import java.util.Arrays;

import org.springframework.web.bind.WebDataBinder;

/**
 * Passwords are bound exactly as typed: the common text normalization (FormBindingAdvice: strip, line breaks,
 * invisible-only to null) does not apply to them. Printing a password back into a form is never done.
 */
final class RawPasswordEditor extends PropertyEditorSupport {

    static void register(WebDataBinder binder, String... fields) {
        Arrays.stream(fields).forEach(field -> binder.registerCustomEditor(String.class, field, new RawPasswordEditor()));
    }

    @Override
    public void setAsText(String text) {
        setValue(text);
    }

    @Override
    public String getAsText() {
        return "";
    }
}
