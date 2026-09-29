package com.plandosee.diary.plan.application;

import java.util.List;

import com.plandosee.diary.common.error.StaleVersionException;
import com.plandosee.diary.plan.domain.PlanRow;

/**
 * The plan edit form was out of date. latest is the plan as stored now (read under the row lock).
 * changedFields uses PlanForm field names (title, startDate, endDate, priority, successCriteria, estimatedMinutes)
 * plus "period" when either date differs.
 */
public class PlanStaleException extends StaleVersionException {

    public static final String CODE = "plan.edit.staleVersion";

    private final transient PlanRow latest;

    public PlanStaleException(PlanRow latest, List<String> changedFields) {
        super(CODE, latest.getVersion(), changedFields);
        this.latest = latest;
    }

    public PlanRow latest() {
        return latest;
    }
}
