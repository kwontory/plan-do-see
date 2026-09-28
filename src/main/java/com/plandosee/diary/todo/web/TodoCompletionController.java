package com.plandosee.diary.todo.web;

import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.plandosee.diary.common.error.ConcurrencyConflictException;
import com.plandosee.diary.common.error.DomainRuleException;
import com.plandosee.diary.common.web.ConflictKeys;
import com.plandosee.diary.common.web.FlashMessages;
import com.plandosee.diary.todo.application.TodoCompletionService;
import com.plandosee.diary.todo.application.TodoDeletedException;
import com.plandosee.diary.todo.application.TodoService;
import com.plandosee.diary.todo.application.TransitionOutcome;
import com.plandosee.diary.todo.domain.TodoStatus;

/**
 * Completion and reopen (DEC-05, ADR-05). Server idempotency does not rely on the browser disabling buttons.
 * The notice and the redirect come from the service's {@link TransitionOutcome}, taken in the same transaction as
 * the change (ADR-14 C-1). A todo deleted by a concurrent request sends the user to its list (ADR-15); a request
 * that kept colliding with others returns to the same screen with a retry notice.
 */
@Controller
public class TodoCompletionController {

    public static final String COMPLETED = "flash.todo.completed";
    public static final String ALREADY_COMPLETED = "flash.todo.alreadyCompleted";
    public static final String REPLAYED_NOW_IN_PROGRESS = "flash.todo.replayedNowInProgress";
    public static final String REOPENED = "flash.todo.reopened";
    public static final String ALREADY_IN_PROGRESS = "flash.todo.alreadyInProgress";

    private final TodoCompletionService completionService;
    private final TodoService todoService;

    public TodoCompletionController(TodoCompletionService completionService, TodoService todoService) {
        this.completionService = completionService;
        this.todoService = todoService;
    }

    @PostMapping("/todos/{id}/completion")
    public String complete(@PathVariable("id") UUID todoId,
                           @RequestParam(name = "idempotencyKey", required = false) String idempotencyKey,
                           @RequestParam(name = "returnTo", required = false) String returnTo,
                           @ModelAttribute("filter") TodoListQuery filter, RedirectAttributes redirect) {
        try {
            TransitionOutcome outcome = completionService.completeWithOutcome(todoId, parseKey(idempotencyKey));
            FlashMessages.add(redirect, messageKey(outcome));
            return redirect(todoId, outcome.planId(), returnTo, filter);
        } catch (TodoDeletedException ex) {
            return deleted(ex, redirect, filter);
        } catch (DomainRuleException ex) {
            FlashMessages.add(redirect, ex.code(), ex.args());
        } catch (ConcurrencyConflictException ex) {
            FlashMessages.add(redirect, ConflictKeys.FLASH_COMPLETION_RETRY);
        }
        return failedRedirect(todoId, returnTo, filter);
    }

    @DeleteMapping("/todos/{id}/completion")
    public String reopen(@PathVariable("id") UUID todoId,
                         @RequestParam(name = "returnTo", required = false) String returnTo,
                         @ModelAttribute("filter") TodoListQuery filter, RedirectAttributes redirect) {
        try {
            TransitionOutcome outcome = completionService.reopenWithOutcome(todoId);
            FlashMessages.add(redirect, messageKey(outcome));
            return redirect(todoId, outcome.planId(), returnTo, filter);
        } catch (TodoDeletedException ex) {
            return deleted(ex, redirect, filter);
        } catch (ConcurrencyConflictException ex) {
            FlashMessages.add(redirect, ConflictKeys.FLASH_RETRY);
        }
        return failedRedirect(todoId, returnTo, filter);
    }

    /**
     * ADR-12 Q-E2: a replayed key adds nothing; the message follows the todo's status at that moment so a request
     * replayed after a reopen is not mistaken for a completion. Returns a message key.
     */
    static String messageKey(TransitionOutcome outcome) {
        return switch (outcome.result()) {
            case REPLAYED -> outcome.currentStatus() == TodoStatus.COMPLETED ? COMPLETED : REPLAYED_NOW_IN_PROGRESS;
            case COMPLETED -> COMPLETED;
            case ALREADY_COMPLETED -> ALREADY_COMPLETED;
            case REOPENED -> REOPENED;
            case ALREADY_IN_PROGRESS -> ALREADY_IN_PROGRESS;
        };
    }

    private static String deleted(TodoDeletedException ex, RedirectAttributes redirect, TodoListQuery filter) {
        FlashMessages.add(redirect, ConflictKeys.FLASH_TODO_ALREADY_DELETED);
        return "redirect:" + filter.listUrl(ex.planId());
    }

    /**
     * Nothing changed; go back to the screen the request came from. The plan id is looked up only to build the
     * list URL (a missing or foreign todo still ends in 404).
     */
    private String failedRedirect(UUID todoId, String returnTo, TodoListQuery filter) {
        if ("list".equals(returnTo)) {
            return "redirect:" + filter.listUrl(todoService.planIdForRedirect(todoId));
        }
        todoService.planIdForRedirect(todoId);
        return "redirect:/todos/" + todoId;
    }

    private static String redirect(UUID todoId, UUID planId, String returnTo, TodoListQuery filter) {
        if ("list".equals(returnTo)) {
            return "redirect:" + filter.listUrl(planId);
        }
        return "redirect:/todos/" + todoId;
    }

    private static UUID parseKey(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.strip());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
