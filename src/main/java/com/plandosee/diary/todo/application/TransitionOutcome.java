package com.plandosee.diary.todo.application;

import java.util.UUID;

import com.plandosee.diary.todo.domain.TodoStatus;

/**
 * Result of a completion or reopen request, taken in the same transaction as the change (ADR-14 C-1):
 * what happened, the todo's status at that moment, and its plan (for the list redirect). The web layer chooses
 * the notice from these values without reading the todo again.
 */
public record TransitionOutcome(TransitionResult result, TodoStatus currentStatus, UUID planId) {
}
