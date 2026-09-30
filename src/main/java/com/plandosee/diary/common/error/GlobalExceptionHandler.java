package com.plandosee.diary.common.error;

import java.util.Optional;

import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.tomcat.util.http.InvalidParameterException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
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
import com.plandosee.diary.common.web.PageAccountModel;

/**
 * Error pages never include stack traces, SQL, or configuration values.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ObjectProvider<PageAccountModel> accountModel;

    public GlobalExceptionHandler(ObjectProvider<PageAccountModel> accountModel) {
        this.accountModel = accountModel;
    }

    /** Page-shell attributes of the logged-in person on every error page (none without login). */
    private String page(Model model, String view, boolean allowDatabaseRead) {
        accountModel.ifAvailable(contributor -> contributor.addTo(model.asMap(), allowDatabaseRead));
        return view;
    }

    @ExceptionHandler({NotFoundException.class, NoResourceFoundException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(Model model) {
        return page(model, "error/404", true);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, DomainRuleException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String badRequest(Model model) {
        return page(model, "error/400", true);
    }

    /**
     * An integrity violation outside the write boundary (the boundary already turns them into rule
     * violations). A 400 notice, never a 500; the log has the constraint name only.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String constraintViolation(DataIntegrityViolationException ex, Model model) {
        log.warn("event=constraint_violation constraint={}",
                ConstraintViolationTranslator.constraintName(ex).orElse("unknown"));
        return page(model, "error/400", true);
    }

    /**
     * Request parameters Tomcat cannot decode (a bad percent escape such as {@code %ZZ}, bytes that
     * are not UTF-8) are the client's fault: 400, not 500. Found anywhere in the cause chain; the log has the type only.
     */
    @ExceptionHandler(InvalidParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String undecodableParameters(InvalidParameterException ex, Model model) {
        log.warn("event=bad_request type={}", ex.getClass().getName());
        return page(model, "error/400", true);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public String methodNotAllowed(Model model) {
        return page(model, "error/400", true);
    }

    /**
     * A request that is not a form or a button (those handle conflicts themselves) and collided with
     * another request. A raw lock failure reaches here only from a path without the write boundary.
     */
    @ExceptionHandler({ConcurrencyConflictException.class, PessimisticLockingFailureException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public String conflict(RuntimeException ex, Model model) {
        if (ex instanceof ConcurrencyConflictException conflict) {
            log.warn("event=concurrency_conflict kind={} attempts={}", conflict.kind(), conflict.attempts());
        } else {
            log.warn("event=concurrency_conflict kind={} attempts=1",
                    TransientConflict.classify(ex).map(Enum::name).orElse("UNKNOWN"));
        }
        return page(model, ConflictKeys.CONFLICT_VIEW, true);
    }

    /**
     * The request did not fit in its time budget (statement or transaction timeout, no pooled connection
     * within the pool wait limit). Not a server fault: HTTP 503 and the error/503 view ("open it again shortly").
     */
    @ExceptionHandler(ServiceBusyException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public String busy(ServiceBusyException ex, Model model) {
        log.warn("event=busy cause={}", ex.busyCause());
        return page(model, ConflictKeys.BUSY_VIEW, false);
    }

    /**
     * Anything else is a 500, except a budget failure raised outside the write boundary (plain reads), which is
     * recognised anywhere in the cause chain and answered like {@link #busy}.
     */
    @ExceptionHandler(Exception.class)
    public String serverError(Exception ex, HttpServletResponse response, Model model) {
        Optional<BusyCause> busy = BusyCause.classify(ex);
        if (busy.isPresent()) {
            log.warn("event=busy cause={}", busy.get());
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            return page(model, ConflictKeys.BUSY_VIEW, false);
        }
        if (causedBy(ex, InvalidParameterException.class)) {
            log.warn("event=bad_request type={}", InvalidParameterException.class.getName());
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return page(model, "error/400", true);
        }
        log.error("event=unhandled_error type={}", ex.getClass().getName());
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        return page(model, "error/500", false);
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
