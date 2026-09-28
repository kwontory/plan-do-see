package com.plandosee.diary.common.web;

/**
 * ADR-15 result codes for concurrency conflicts and races (message keys, ADR-13). The text is written by
 * Frontend in messages.properties; see docs/architecture/message-keys.md.
 */
public final class ConflictKeys {

    /** Global form error: saving collided with another request after every retry; the input is kept (HTTP 409). */
    public static final String FORM_RETRY = "concurrency.form.retry";
    /** Flash after a button request (reopen, delete, review create) collided after every retry. */
    public static final String FLASH_RETRY = "flash.concurrency.retry";
    /** Flash after a completion collided after every retry; pressing again never duplicates the completion. */
    public static final String FLASH_COMPLETION_RETRY = "flash.todo.completionRetry";
    /** Flash when completion, reopen, or an execution record targeted a todo that had just been deleted. */
    public static final String FLASH_TODO_ALREADY_DELETED = "flash.todo.alreadyDeleted";
    /** View for any other request that collided after every retry (template owned by Frontend). */
    public static final String CONFLICT_VIEW = "error/409";

    private ConflictKeys() {
    }
}
