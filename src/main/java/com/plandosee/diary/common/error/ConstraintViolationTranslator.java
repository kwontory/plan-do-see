package com.plandosee.diary.common.error;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Last line of defence: a CHECK, UNIQUE, NOT NULL, or foreign-key violation that got past the service
 * checks is never a 500. A known constraint becomes the error code its feature registered (ConstraintCodeSource),
 * marked on the matching form field; any other becomes the global code {@link #SAVE_REJECTED}. Forms show it with
 * the input kept, other requests get the 400 page.
 * <p>
 * The log line carries the constraint name only: never the SQL, the values, or the driver message.
 */
@Component
public class ConstraintViolationTranslator {

    /** Global error: the data could not be saved because a stored-data rule rejected it. */
    public static final String SAVE_REJECTED = "data.save.rejected";
    /** Field error: this value cannot be stored (a DB rule without a more specific field code). */
    public static final String VALUE_INVALID = "data.value.invalid";
    static final String UNKNOWN = "unknown";

    private static final Logger log = LoggerFactory.getLogger(ConstraintViolationTranslator.class);
    private static final Pattern QUOTED_CONSTRAINT = Pattern.compile("constraint \"([A-Za-z0-9_]+)\"");

    private final Map<String, ConstraintCode> known = new HashMap<>();

    public ConstraintViolationTranslator(List<ConstraintCodeSource> sources) {
        for (ConstraintCodeSource source : sources) {
            for (ConstraintCode code : source.constraintCodes()) {
                if (known.putIfAbsent(code.constraint(), code) != null) {
                    throw new IllegalStateException("constraint registered twice: " + code.constraint());
                }
            }
        }
    }

    /** The rule violation to show instead of the DB error. Logs the constraint name at WARN. */
    public DomainRuleException translate(DataIntegrityViolationException failure) {
        String name = constraintName(failure).orElse(UNKNOWN);
        log.warn("event=constraint_violation constraint={}", name);
        ConstraintCode code = known.get(name);
        if (code == null) {
            return new DomainRuleException(null, SAVE_REJECTED);
        }
        return new DomainRuleException(code.field(), code.code(), code.args());
    }

    /** True when the constraint has a registered code (tests). */
    public boolean isKnown(String constraint) {
        return known.containsKey(constraint);
    }

    /**
     * Constraint name of a PostgreSQL integrity error anywhere in the cause chain: first from the server error
     * fields (pgjdbc ServerErrorMessage.getConstraint, read reflectively because the driver is a runtime-only
     * dependency), then from the English message text.
     */
    public static Optional<String> constraintName(Throwable failure) {
        for (Throwable t = failure; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof SQLException sql) {
                Optional<String> fromServer = fromServerErrorMessage(sql);
                if (fromServer.isPresent()) {
                    return fromServer;
                }
                if (sql.getMessage() != null) {
                    Matcher matcher = QUOTED_CONSTRAINT.matcher(sql.getMessage());
                    if (matcher.find()) {
                        return Optional.of(matcher.group(1));
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<String> fromServerErrorMessage(SQLException sql) {
        try {
            Method serverMessage = sql.getClass().getMethod("getServerErrorMessage");
            Object message = serverMessage.invoke(sql);
            if (message == null) {
                return Optional.empty();
            }
            Object constraint = message.getClass().getMethod("getConstraint").invoke(message);
            return constraint instanceof String name && !name.isBlank() ? Optional.of(name) : Optional.empty();
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return Optional.empty();
        }
    }
}
