package com.plandosee.diary.common.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.plandosee.diary.common.concurrency.TransientConflict;
import com.plandosee.diary.common.web.ConflictKeys;

/**
 * Error pages never include stack traces, SQL, or configuration values.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({NotFoundException.class, NoResourceFoundException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound() {
        return "error/404";
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, DomainRuleException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String badRequest() {
        return "error/400";
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public String methodNotAllowed() {
        return "error/400";
    }

    /**
     * ADR-15: a request that is not a form or a button (those handle conflicts themselves) and still collided with
     * other requests. A raw lock failure reaches here only from a path without the retrying write boundary.
     */
    @ExceptionHandler({ConcurrencyConflictException.class, PessimisticLockingFailureException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public String conflict(RuntimeException ex) {
        if (ex instanceof ConcurrencyConflictException conflict) {
            log.warn("event=concurrency_conflict kind={} attempts={}", conflict.kind(), conflict.attempts());
        } else {
            log.warn("event=concurrency_conflict kind={} attempts=1",
                    TransientConflict.classify(ex).map(Enum::name).orElse("UNKNOWN"));
        }
        return ConflictKeys.CONFLICT_VIEW;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String serverError(Exception ex) {
        log.error("event=unhandled_error type={}", ex.getClass().getName());
        return "error/500";
    }
}
