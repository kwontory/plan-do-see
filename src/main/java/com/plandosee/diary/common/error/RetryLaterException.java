package com.plandosee.diary.common.error;

/**
 * A request that failed for a temporary reason and changed nothing, so the user can simply try again.
 * The web layer never shows a 500 page for it: a form is shown again with the input kept, a button
 * redirects with a flash code, anything else gets an error view. Subtypes carry only a kind; never SQL,
 * parameters, or connection details.
 */
public abstract class RetryLaterException extends RuntimeException {

    protected RetryLaterException(String message, Throwable cause) {
        super(message, cause, false, false);
    }
}
