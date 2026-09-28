package com.plandosee.diary.common.web;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.validation.BindingResult;

import com.plandosee.diary.common.error.RetryLaterException;
import com.plandosee.diary.common.error.ServiceBusyException;

/**
 * ADR-15 / ADR-19: a request that failed for a temporary reason changed nothing.
 * <ul>
 *   <li>Form: shown again with the submitted input and a global error code; HTTP 409 for a collision with another
 *       request, HTTP 503 when the time budget ran out.</li>
 *   <li>Button: redirect with a flash code.</li>
 * </ul>
 * The code is also passed as the default message. The template resolves the code through messages.properties
 * first; the default only prevents a rendering failure (NoSuchMessageException, a 500 page) if the text has not
 * been written yet. MessageCatalogTest reports such a missing text.
 */
public final class ConflictResponses {

    private ConflictResponses() {
    }

    public static void rejectForm(BindingResult result, HttpServletResponse response, RetryLaterException failure) {
        boolean busy = failure instanceof ServiceBusyException;
        String code = busy ? ConflictKeys.BUSY_FORM_RETRY : ConflictKeys.FORM_RETRY;
        result.reject(code, null, code);
        response.setStatus(busy ? HttpServletResponse.SC_SERVICE_UNAVAILABLE : HttpServletResponse.SC_CONFLICT);
    }

    /** Flash code for a button request: the given collision code, or the busy code when the budget ran out. */
    public static String flashKey(RetryLaterException failure, String conflictKey) {
        return failure instanceof ServiceBusyException ? ConflictKeys.FLASH_BUSY_RETRY : conflictKey;
    }
}
