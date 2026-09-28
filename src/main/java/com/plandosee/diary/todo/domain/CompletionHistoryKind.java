package com.plandosee.diary.todo.domain;

/**
 * ADR-16: kind of a row in a todo's completion history. Display labels live in messages.properties as
 * enum.CompletionHistoryKind.&lt;NAME&gt; (ADR-13).
 */
public enum CompletionHistoryKind {
    COMPLETED,
    REOPENED
}
