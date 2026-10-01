package com.plandosee.diary.execution.application;

import java.util.List;

import com.plandosee.diary.common.error.StaleVersionException;
import com.plandosee.diary.execution.domain.ExecutionLogRow;

/**
 * The execution record edit form was out of date. latest is the record as stored now (read under
 * the row lock); changedFields uses ExecutionForm field names (startedAt, endedAt, blockerReason).
 */
public class ExecutionStaleException extends StaleVersionException {

    public static final String CODE = "execution.edit.staleVersion";

    private final transient ExecutionLogRow latest;

    public ExecutionStaleException(ExecutionLogRow latest, List<String> changedFields) {
        super(CODE, latest.getVersion(), changedFields);
        this.latest = latest;
    }

    public ExecutionLogRow latest() {
        return latest;
    }
}
