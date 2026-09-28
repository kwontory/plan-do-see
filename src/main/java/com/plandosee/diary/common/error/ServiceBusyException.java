package com.plandosee.diary.common.error;

import com.plandosee.diary.common.concurrency.BusyCause;

/**
 * ADR-19: the request did not fit in its time budget (a statement ran past statement_timeout, the transaction
 * outlived the request budget, or no pooled connection became free within the pool wait limit). Nothing was
 * committed. Forms are re-rendered with the input kept (HTTP 503), buttons redirect with a flash code, anything
 * else gets the error/503 view. Carries only the cause kind.
 */
public class ServiceBusyException extends RetryLaterException {

    private final BusyCause busyCause;

    public ServiceBusyException(BusyCause busyCause, Throwable cause) {
        super("service busy cause=" + busyCause, cause);
        this.busyCause = busyCause;
    }

    public BusyCause busyCause() {
        return busyCause;
    }
}
