package com.plandosee.diary.common.db;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.plandosee.diary.common.db.port.SessionSettingsMapper;

/**
 * ADR-19 query time limits. Every pooled session starts with the general statement_timeout (1s by default).
 * Aggregate and statistics reads (review summary and detail, evidence, export) widen it for their own read
 * transaction only, to app.db.aggregate-statement-timeout (2s by default); the pool default returns at commit.
 */
@Component
public class StatementBudget {

    private final SessionSettingsMapper settings;
    private final String aggregateTimeout;

    public StatementBudget(SessionSettingsMapper settings,
                           @Value("${" + DbTimeoutSettings.AGGREGATE_STATEMENT_TIMEOUT + "}") String aggregateTimeout) {
        this.settings = settings;
        this.aggregateTimeout = DbTimeoutSettings.requireDuration(DbTimeoutSettings.AGGREGATE_STATEMENT_TIMEOUT,
                aggregateTimeout);
    }

    /** Must be called inside the aggregate read transaction; the setting ends with that transaction. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void useAggregateTimeout() {
        settings.setLocalStatementTimeout(aggregateTimeout);
    }
}
