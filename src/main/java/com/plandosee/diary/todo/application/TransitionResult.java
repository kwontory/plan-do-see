package com.plandosee.diary.todo.application;

/**
 * Outcome of a completion or reopen request. REPLAYED means the same idempotency key was already applied;
 * ALREADY_COMPLETED means another key had completed the todo. Neither adds a completion event.
 */
public enum TransitionResult {
    COMPLETED,
    REPLAYED,
    ALREADY_COMPLETED,
    REOPENED,
    ALREADY_IN_PROGRESS
}
