package com.plandosee.diary.common.web;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.validation.BindingResult;

/**
 * ADR-15: a form whose save collided with other requests after every retry is shown again with the submitted
 * input and a global error code, with HTTP 409.
 * <p>
 * The code is also passed as the default message. The template resolves the code through messages.properties
 * first; the default only prevents a rendering failure (NoSuchMessageException, a 500 page) if the text has not
 * been written yet. MessageCatalogTest reports such a missing text.
 */
public final class ConflictResponses {

    private ConflictResponses() {
    }

    public static void rejectForm(BindingResult result, HttpServletResponse response) {
        result.reject(ConflictKeys.FORM_RETRY, null, ConflictKeys.FORM_RETRY);
        response.setStatus(HttpServletResponse.SC_CONFLICT);
    }
}
