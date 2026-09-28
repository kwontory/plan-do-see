package com.plandosee.diary.todo.web;

import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.plandosee.diary.common.error.DomainRuleException;
import com.plandosee.diary.todo.application.TodoCompletionService;
import com.plandosee.diary.todo.application.TodoService;
import com.plandosee.diary.todo.application.TransitionResult;
import com.plandosee.diary.todo.domain.TodoStatus;

/**
 * Completion and reopen (DEC-05, ADR-05). Server idempotency does not rely on the browser disabling buttons.
 */
@Controller
public class TodoCompletionController {

    static final String COMPLETED = "완료로 바꿨습니다.";
    static final String ALREADY_COMPLETED = "이미 완료된 할 일입니다. 완료 기록은 추가되지 않았습니다.";
    static final String REPLAYED_NOW_IN_PROGRESS = "이미 처리한 완료 요청입니다. 이 할 일은 지금 진행 중입니다.";
    static final String REOPENED = "진행 중으로 되돌렸습니다.";
    static final String ALREADY_IN_PROGRESS = "이미 진행 중인 할 일입니다.";

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
        UUID planId = todoService.get(todoId).getPlanId();
        String message;
        try {
            TransitionResult result = completionService.complete(todoId, parseKey(idempotencyKey));
            message = result == TransitionResult.REPLAYED
                    ? replayMessage(todoService.get(todoId).getStatus())
                    : message(result);
        } catch (DomainRuleException ex) {
            message = ex.getMessage();
        }
        redirect.addFlashAttribute("flashMessage", message);
        return redirect(todoId, planId, returnTo, filter);
    }

    @DeleteMapping("/todos/{id}/completion")
    public String reopen(@PathVariable("id") UUID todoId,
                         @RequestParam(name = "returnTo", required = false) String returnTo,
                         @ModelAttribute("filter") TodoListQuery filter, RedirectAttributes redirect) {
        UUID planId = todoService.get(todoId).getPlanId();
        redirect.addFlashAttribute("flashMessage", message(completionService.reopen(todoId)));
        return redirect(todoId, planId, returnTo, filter);
    }

    /**
     * ADR-12 Q-E2: a replayed key adds nothing; the message follows the todo's current state so a request
     * replayed after a reopen is not mistaken for a completion.
     */
    static String replayMessage(TodoStatus currentStatus) {
        return currentStatus == TodoStatus.COMPLETED ? COMPLETED : REPLAYED_NOW_IN_PROGRESS;
    }

    static String message(TransitionResult result) {
        return switch (result) {
            case COMPLETED, REPLAYED -> COMPLETED;
            case ALREADY_COMPLETED -> ALREADY_COMPLETED;
            case REOPENED -> REOPENED;
            case ALREADY_IN_PROGRESS -> ALREADY_IN_PROGRESS;
        };
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
