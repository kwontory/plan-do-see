package com.plandosee.diary.common.db;

import java.util.regex.Pattern;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * ADR-19 time limits. The PostgreSQL values end up inside connection-init-sql and set_config, so every one must be
 * a plain duration (digits and an optional ms/s/min unit) before any bean, and thus any pooled connection, is
 * created. A malformed value stops the start-up with the property name only (never the value, which may have been
 * set by mistake to something sensitive).
 */
@Component
public class DbTimeoutSettings implements BeanFactoryPostProcessor, EnvironmentAware {

    public static final String LOCK_TIMEOUT = "app.db.lock-timeout";
    public static final String STATEMENT_TIMEOUT = "app.db.statement-timeout";
    public static final String AGGREGATE_STATEMENT_TIMEOUT = "app.db.aggregate-statement-timeout";
    public static final String REQUEST_BUDGET_MS = "app.request-budget-ms";

    /** Transaction timeouts are whole seconds, so the request budget cannot be shorter than one second. */
    public static final long MIN_REQUEST_BUDGET_MS = 1000;

    private static final Pattern DURATION = Pattern.compile("[0-9]{1,9}(ms|s|min)?");

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        for (String name : new String[] {LOCK_TIMEOUT, STATEMENT_TIMEOUT, AGGREGATE_STATEMENT_TIMEOUT}) {
            requireDuration(name, environment.getProperty(name));
        }
        requireBudget(environment.getProperty(REQUEST_BUDGET_MS));
    }

    public static String requireDuration(String name, String value) {
        if (value == null || !DURATION.matcher(value).matches()) {
            throw new IllegalStateException(name + " must be a PostgreSQL duration such as 100ms, 1s, or 2s");
        }
        return value;
    }

    public static long requireBudget(String value) {
        long budget;
        try {
            budget = value == null ? -1 : Long.parseLong(value.strip());
        } catch (NumberFormatException ex) {
            budget = -1;
        }
        if (budget < MIN_REQUEST_BUDGET_MS) {
            throw new IllegalStateException(REQUEST_BUDGET_MS + " must be a whole number of milliseconds, at least "
                    + MIN_REQUEST_BUDGET_MS);
        }
        return budget;
    }
}
