package com.plandosee.diary.review.domain;

import java.util.List;

import com.plandosee.diary.common.domain.DurationParts;
import com.plandosee.diary.common.paging.PageInfo;
import com.plandosee.diary.execution.domain.ExecutionLogRow;

/**
 * Drill-down result that reconciles with the summary on the same page (T06-C83, web-contract revision 1 Q2).
 * todos and logs are the rows of the requested page (ADR-21); every number below covers the whole scope, never
 * just the page, so it keeps matching the review metric.
 * <ul>
 *   <li>evidenceCount: number of logs for actual, otherwise number of todos (= the count metric for planned,
 *       completed, overdue, blocked)</li>
 *   <li>evidenceEstimatedMinutes: estimate sum of the listed todos (= estimatedMinutes for estimated, variance)</li>
 *   <li>evidenceActualMinutes: log sum for actual; otherwise the sum of the listed todos' execution totals
 *       (= actualMinutes for variance). variance = evidenceActualMinutes - evidenceEstimatedMinutes.</li>
 *   <li>todoPage: position of the todo list page; null for actual (no todo list). logPage: position of the log
 *       list page; null unless the metric shows a log list (actual, variance).</li>
 * </ul>
 * For blocked, logs contains only the logs with a blocker reason of the todos on the page (they are shown inside
 * the todo rows).
 */
public record ReviewEvidence(
        ReviewMetric metric,
        ReviewSummary summary,
        List<EvidenceTodo> todos,
        List<ExecutionLogRow> logs,
        int evidenceCount,
        long evidenceEstimatedMinutes,
        long evidenceActualMinutes,
        PageInfo todoPage,
        PageInfo logPage) {

    public long evidenceVarianceMinutes() {
        return evidenceActualMinutes - evidenceEstimatedMinutes;
    }

    /** ADR-29: the evidence sums split into days, hours and minutes for display. */
    public DurationParts evidenceEstimatedDuration() {
        return DurationParts.of(evidenceEstimatedMinutes);
    }

    public DurationParts evidenceActualDuration() {
        return DurationParts.of(evidenceActualMinutes);
    }

    public DurationParts evidenceVarianceDuration() {
        return DurationParts.of(evidenceVarianceMinutes());
    }

    /**
     * True when the metric has no evidence row at all (ADR-17 F-2), over the whole scope rather than the page:
     * actual shows only logs, variance shows todos and logs, every other metric shows only todos (blocked logs are
     * shown inside the todo rows).
     */
    public boolean empty() {
        return switch (metric) {
            case ACTUAL -> total(logPage) == 0;
            case VARIANCE -> total(todoPage) == 0 && total(logPage) == 0;
            default -> total(todoPage) == 0;
        };
    }

    private static long total(PageInfo page) {
        return page == null ? 0 : page.totalCount();
    }
}
