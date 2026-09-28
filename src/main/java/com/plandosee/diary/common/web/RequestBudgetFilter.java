package com.plandosee.diary.common.web;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import com.plandosee.diary.common.db.DbTimeoutSettings;

/**
 * ADR-19 page budget: a request that took longer than app.request-budget-ms (3000 by default) is logged at WARN
 * with the route template (for example {@code /todos/{id}}) and the elapsed milliseconds only. Never the actual
 * path, query string, parameters, user values, or SQL (CLAUDE.md 5장).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestBudgetFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestBudgetFilter.class);
    static final String UNMATCHED = "unmatched";

    private final long budgetMillis;
    private final LongSupplier nanoClock;

    @Autowired
    public RequestBudgetFilter(@Value("${" + DbTimeoutSettings.REQUEST_BUDGET_MS + "}") String budgetMillis) {
        this(DbTimeoutSettings.requireBudget(budgetMillis), System::nanoTime);
    }

    RequestBudgetFilter(long budgetMillis, LongSupplier nanoClock) {
        this.budgetMillis = budgetMillis;
        this.nanoClock = nanoClock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long start = nanoClock.getAsLong();
        try {
            chain.doFilter(request, response);
        } finally {
            long elapsed = TimeUnit.NANOSECONDS.toMillis(nanoClock.getAsLong() - start);
            if (elapsed > budgetMillis) {
                log.warn("event=slow_request method={} route={} elapsedMs={} budgetMs={}",
                        request.getMethod(), route(request), elapsed, budgetMillis);
            }
        }
    }

    static String route(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern instanceof String template ? template : UNMATCHED;
    }

    long budgetMillis() {
        return budgetMillis;
    }
}
