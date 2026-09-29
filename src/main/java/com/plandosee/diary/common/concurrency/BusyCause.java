package com.plandosee.diary.common.concurrency;

import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.Optional;

import org.springframework.dao.QueryTimeoutException;
import org.springframework.transaction.TransactionTimedOutException;

/**
 * A failure caused by the time budget rather than by the request itself. Classified anywhere in the cause
 * chain, so the same failure is recognised whether it surfaces from MyBatis, the transaction manager, or JDBC.
 * <ul>
 *   <li>QUERY_TIMEOUT: SQLSTATE 57014 (statement_timeout, or a JDBC query timeout derived from the transaction
 *       deadline), or Spring's {@link QueryTimeoutException}</li>
 *   <li>TRANSACTION_TIMEOUT: {@link TransactionTimedOutException}, the transaction deadline passed between
 *       statements</li>
 *   <li>CONNECTION_WAIT: {@link SQLTransientConnectionException}, no pooled connection within
 *       spring.datasource.hikari.connection-timeout</li>
 * </ul>
 * Not a {@link TransientConflict}: repeating a slow request at once only adds load.
 */
public enum BusyCause {
    QUERY_TIMEOUT,
    TRANSACTION_TIMEOUT,
    CONNECTION_WAIT;

    public static final String QUERY_CANCELED_SQLSTATE = "57014";

    public static Optional<BusyCause> classify(Throwable failure) {
        for (Throwable t = failure; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof TransactionTimedOutException) {
                return Optional.of(TRANSACTION_TIMEOUT);
            }
            if (t instanceof SQLTransientConnectionException) {
                return Optional.of(CONNECTION_WAIT);
            }
            if (t instanceof QueryTimeoutException) {
                return Optional.of(QUERY_TIMEOUT);
            }
            if (t instanceof SQLException sql && QUERY_CANCELED_SQLSTATE.equals(sql.getSQLState())) {
                return Optional.of(QUERY_TIMEOUT);
            }
        }
        return Optional.empty();
    }
}
