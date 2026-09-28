package com.plandosee.diary.execution.web;

import java.util.UUID;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.plandosee.diary.common.error.RetryLaterException;
import com.plandosee.diary.common.error.DomainRuleException;
import com.plandosee.diary.common.web.ConflictKeys;
import com.plandosee.diary.common.web.ConflictResponses;
import com.plandosee.diary.common.web.FlashMessages;
import com.plandosee.diary.common.web.FormErrors;
import com.plandosee.diary.execution.application.ExecutionService;
import com.plandosee.diary.todo.application.TodoDeletedException;
import com.plandosee.diary.todo.web.TodoListQuery;
import com.plandosee.diary.todo.web.TodoPageModels;

/**
 * S03 execution record. A rejected range (end before start) re-renders the detail page with a field error.
 * A todo deleted by a concurrent request sends the user to its list; a save that kept colliding with other
 * requests re-renders the form with the input kept and HTTP 409 (ADR-15).
 */
@Controller
public class ExecutionController {

    public static final String FLASH_RECORDED = "flash.execution.recorded";

    private final ExecutionService executionService;
    private final TodoPageModels pages;

    public ExecutionController(ExecutionService executionService, TodoPageModels pages) {
        this.executionService = executionService;
        this.pages = pages;
    }

    @PostMapping("/todos/{id}/executions")
    public String record(@PathVariable("id") UUID todoId, @Valid @ModelAttribute("executionForm") ExecutionForm form,
                         BindingResult result, Model model, RedirectAttributes redirect,
                         HttpServletResponse response) {
        if (result.hasErrors()) {
            return pages.detail(model, todoId, form);
        }
        try {
            executionService.record(todoId, form.startedAtInSeoul(), form.endedAtInSeoul(), form.getBlockerReason());
        } catch (TodoDeletedException ex) {
            FlashMessages.add(redirect, ConflictKeys.FLASH_TODO_ALREADY_DELETED);
            return "redirect:" + new TodoListQuery().listUrl(ex.planId());
        } catch (DomainRuleException ex) {
            FormErrors.reject(result, ex);
            return pages.detail(model, todoId, form);
        } catch (RetryLaterException ex) {
            ConflictResponses.rejectForm(result, response, ex);
            return pages.detail(model, todoId, form);
        }
        FlashMessages.add(redirect, FLASH_RECORDED);
        return "redirect:/todos/" + todoId;
    }
}
