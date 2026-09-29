package com.plandosee.diary.todo.application;

import java.util.UUID;

import com.plandosee.diary.common.error.NotFoundException;

/**
 * The owned todo was soft-deleted (typically by a concurrent request) before this request could act on it.
 * Still a not-found case for every other caller (404), but completion, reopen, and execution recording turn it
 * into a redirect to the plan's todo list with a notice. planId is the owned, active plan the todo belonged to.
 */
public class TodoDeletedException extends NotFoundException {

    private final UUID planId;

    public TodoDeletedException(UUID planId) {
        super("todo");
        this.planId = planId;
    }

    public UUID planId() {
        return planId;
    }
}
