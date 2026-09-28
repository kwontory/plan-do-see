package com.plandosee.diary.common.web;

/**
 * ADR-15 / ADR-19 result codes for requests that changed nothing and can simply be tried again (message keys,
 * ADR-13). The text is written by Frontend in messages.properties; see docs/architecture/message-keys.md.
 */
public final class ConflictKeys {

    /** Global form error: saving collided with another request; the input is kept (HTTP 409). */
    public static final String FORM_RETRY = "concurrency.form.retry";
    /** Flash after a button request (reopen, delete, review create) collided with another request. */
    public static final String FLASH_RETRY = "flash.concurrency.retry";
    /** Flash after a completion collided with another request; pressing again never duplicates the completion. */
    public static final String FLASH_COMPLETION_RETRY = "flash.todo.completionRetry";
    /** Flash when completion, reopen, or an execution record targeted a todo that had just been deleted. */
    public static final String FLASH_TODO_ALREADY_DELETED = "flash.todo.alreadyDeleted";
    /** View for any other request that collided with another request (template owned by Frontend). */
    public static final String CONFLICT_VIEW = "error/409";

    /** ADR-19 global form error: saving did not fit in the time budget (server busy); the input is kept (HTTP 503). */
    public static final String BUSY_FORM_RETRY = "busy.form.retry";
    /** ADR-19 flash after any button request (completion included) did not fit in the time budget. */
    public static final String FLASH_BUSY_RETRY = "flash.busy.retry";
    /** ADR-19 view (HTTP 503) for any other request that did not fit in the time budget (template owned by Frontend). */
    public static final String BUSY_VIEW = "error/503";

    private ConflictKeys() {
    }
}
