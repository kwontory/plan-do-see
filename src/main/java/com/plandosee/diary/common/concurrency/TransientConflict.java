package com.plandosee.diary.common.concurrency;

import java.sql.SQLException;
import java.util.Optional;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;

/**
 * ADR-15: a failure that disappears when the whole transaction is run again. Classified by PostgreSQL SQLSTATE
 * anywhere in the cause chain (so commit-time failures wrapped by the transaction manager are recognised too),
 * then by the Spring exception hierarchy.
 * <ul>
 *   <li>DEADLOCK: 40P01 ({@code DeadlockLoserDataAccessException}, a {@link PessimisticLockingFailureException})</li>
 *   <li>SERIALIZATION: 40001 ({@code CannotSerializeTransactionException}, a {@link PessimisticLockingFailureException})</li>
 *   <li>LOCK_TIMEOUT: 55P03 ({@link CannotAcquireLockException}), raised at once by {@code FOR UPDATE NOWAIT}
 *       when another request holds the row, or when {@code lock_timeout} expires on an implicit lock wait
 *       (ADR-19)</li>
 * </ul>
 * statement_timeout (57014) is deliberately not transient: a slow statement is not fixed by repeating it.
 */
public enum TransientConflict {
    DEADLOCK("40P01"),
    SERIALIZATION("40001"),
    LOCK_TIMEOUT("55P03");

    private final String sqlState;

    TransientConflict(String sqlState) {
        this.sqlState = sqlState;
    }

    public String sqlState() {
        return sqlState;
    }

    public static Optional<TransientConflict> classify(Throwable failure) {
        for (Throwable t = failure; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof SQLException sql && sql.getSQLState() != null) {
                for (TransientConflict kind : values()) {
                    if (kind.sqlState.equals(sql.getSQLState())) {
                        return Optional.of(kind);
                    }
                }
            }
        }
        if (failure instanceof CannotAcquireLockException) {
            return Optional.of(LOCK_TIMEOUT);
        }
        if (failure instanceof PessimisticLockingFailureException) {
            return Optional.of(DEADLOCK);
        }
        return Optional.empty();
    }
}
