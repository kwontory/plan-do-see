package com.plandosee.diary.review.domain;

import java.util.List;

import com.plandosee.diary.execution.domain.ExecutionLogRow;

/**
 * Drill-down result that reconciles with the summary on the same page (T06-C83, web-contract revision 1 Q2).
 * <ul>
 *   <li>evidenceCount: logs.size() for actual, otherwise todos.size() (= the count metric for planned,
 *       completed, overdue, blocked)</li>
 *   <li>evidenceEstimatedMinutes: estimate sum of the listed todos (= estimatedMinutes for estimated, variance)</li>
 *   <li>evidenceActualMinutes: log sum for actual; otherwise the sum of the listed todos' execution totals
 *       (= actualMinutes for variance). variance = evidenceActualMinutes - evidenceEstimatedMinutes.</li>
 * </ul>
 * For blocked, logs contains only the logs with a blocker reason.
 */
public record ReviewEvidence(
        ReviewMetric metric,
        ReviewSummary summary,
        List<EvidenceTodo> todos,
        List<ExecutionLogRow> logs,
        int evidenceCount,
        long evidenceEstimatedMinutes,
        long evidenceActualMinutes) {

    public long evidenceVarianceMinutes() {
        return evidenceActualMinutes - evidenceEstimatedMinutes;
    }
}
