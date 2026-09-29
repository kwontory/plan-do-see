package com.plandosee.diary.common.error;

import java.util.Optional;

import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.tomcat.util.http.InvalidParameterException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.plandosee.diary.common.concurrency.BusyCause;
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

    /**
     * ADR-22: an integrity violation outside the write boundary (the boundary already turns them into rule
     * violations). A 400 notice, never a 500; the log has the constraint name only.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String constraintViolation(DataIntegrityViolationException ex) {
        log.warn("event=constraint_violation constraint={}",
                ConstraintViolationTranslator.constraintName(ex).orElse("unknown"));
        return "error/400";
    }

    /**
     * ADR-30 (IV-05): request parameters Tomcat cannot decode (a bad percent escape such as {@code %ZZ}, bytes that
     * are not UTF-8) are the client's fault: 400, not 500. Found anywhere in the cause chain; the log has the type only.
     */
    @ExceptionHandler(InvalidParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String undecodableParameters(InvalidParameterException ex) {
        log.warn("event=bad_request type={}", ex.getClass().getName());
        return "error/400";
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public String methodNotAllowed() {
        return "error/400";
    }

    /**
     * ADR-15/ADR-19: a request that is not a form or a button (those handle conflicts themselves) and collided with
     * another request. A raw lock failure reaches here only from a path without the write boundary.
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

    /**
     * ADR-19: the request did not fit in its time budget (statement or transaction timeout, no pooled connection
     * within the pool wait limit). Not a server fault: HTTP 503 and the error/503 view ("open it again shortly").
     */
    @ExceptionHandler(ServiceBusyException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public String busy(ServiceBusyException ex) {
        log.warn("event=busy cause={}", ex.busyCause());
        return ConflictKeys.BUSY_VIEW;
    }

    /**
     * Anything else is a 500, except a budget failure raised outside the write boundary (plain reads), which is
     * recognised anywhere in the cause chain and answered like {@link #busy}.
     */
    @ExceptionHandler(Exception.class)
    public String serverError(Exception ex, HttpServletResponse response) {
        Optional<BusyCause> busy = BusyCause.classify(ex);
        if (busy.isPresent()) {
            log.warn("event=busy cause={}", busy.get());
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            return ConflictKeys.BUSY_VIEW;
        }
        if (causedBy(ex, InvalidParameterException.class)) {
            log.warn("event=bad_request type={}", InvalidParameterException.class.getName());
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return "error/400";
        }
        log.error("event=unhandled_error type={}", ex.getClass().getName());
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        return "error/500";
    }

    private static boolean causedBy(Throwable ex, Class<? extends Throwable> type) {
        for (Throwable t = ex; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (type.isInstance(t)) {
                return true;
            }
        }
        return false;
    }
}
