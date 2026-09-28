package com.plandosee.diary.common.error;

import com.plandosee.diary.common.concurrency.TransientConflict;

/**
 * ADR-15: a write kept colliding with other requests (deadlock, serialization failure, lock wait timeout) after
 * every automatic retry. Nothing was committed. The web layer shows a retry notice instead of a 500 page:
 * forms are re-rendered with the input kept (HTTP 409), buttons redirect with a flash key, anything else gets
 * the error/409 view. Carries only the kind and attempt count; never SQL, parameters, or connection details.
 */
public class ConcurrencyConflictException extends RuntimeException {

    private final TransientConflict kind;
    private final int attempts;

    public ConcurrencyConflictException(TransientConflict kind, int attempts, Throwable cause) {
        super("concurrency conflict kind=" + kind + " attempts=" + attempts, cause, false, false);
        this.kind = kind;
        this.attempts = attempts;
    }

    public TransientConflict kind() {
        return kind;
    }

    public int attempts() {
        return attempts;
    }
}
