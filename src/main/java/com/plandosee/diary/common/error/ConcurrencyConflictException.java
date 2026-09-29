package com.plandosee.diary.common.error;

import com.plandosee.diary.common.concurrency.TransientConflict;

/**
 * A write collided with another request (row held by another request: NOWAIT or lock_timeout,
 * deadlock, serialization failure) and every automatic retry (none by default) failed too. Nothing was
 * committed. Forms are re-rendered with the input kept (HTTP 409), buttons redirect with a flash key, anything
 * else gets the error/409 view. Carries only the kind and attempt count.
 */
public class ConcurrencyConflictException extends RetryLaterException {

    private final TransientConflict kind;
    private final int attempts;

    public ConcurrencyConflictException(TransientConflict kind, int attempts, Throwable cause) {
        super("concurrency conflict kind=" + kind + " attempts=" + attempts, cause);
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
