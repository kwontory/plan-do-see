package com.plandosee.diary.todo.domain;

/**
 * Kind of a row in a todo's completion history. Display labels live in messages.properties as
 * enum.CompletionHistoryKind.&lt;NAME&gt;.
 */
public enum CompletionHistoryKind {
    COMPLETED,
    REOPENED
}
