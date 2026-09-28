package com.plandosee.diary.todo.application;

import java.util.List;

import com.plandosee.diary.common.error.StaleVersionException;
import com.plandosee.diary.todo.domain.TodoRow;

/**
 * ADR-18: the todo edit form was out of date. latest is the todo as stored now, with its tags and status (status is
 * shown but never compared, E7). changedFields uses TodoForm field names (title, dueDate, priority,
 * estimatedMinutes, tags; tags compared by normalized name).
 */
public class TodoStaleException extends StaleVersionException {

    public static final String CODE = "todo.edit.staleVersion";

    private final transient TodoRow latest;

    public TodoStaleException(TodoRow latest, List<String> changedFields) {
        super(CODE, latest.getVersion(), changedFields);
        this.latest = latest;
    }

    public TodoRow latest() {
        return latest;
    }
}
