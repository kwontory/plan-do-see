package com.plandosee.diary.todo.web;

import java.util.UUID;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.plandosee.diary.common.domain.Priority;
import com.plandosee.diary.common.error.ConcurrencyConflictException;
import com.plandosee.diary.common.error.DomainRuleException;
import com.plandosee.diary.common.web.ConflictKeys;
import com.plandosee.diary.common.web.ConflictResponses;
import com.plandosee.diary.common.web.FlashMessages;
import com.plandosee.diary.common.web.FormErrors;
import com.plandosee.diary.execution.web.ExecutionForm;
import com.plandosee.diary.todo.application.TodoService;
import com.plandosee.diary.todo.domain.TodoRow;

/**
 * S02 list/create/edit/delete and S03 detail.
 */
@Controller
public class TodoController {

    public static final String FLASH_CREATED = "flash.todo.created";
    public static final String FLASH_UPDATED = "flash.todo.updated";
    public static final String FLASH_DELETED = "flash.todo.deleted";

    private final TodoService todoService;
    private final TodoPageModels pages;

    public TodoController(TodoService todoService, TodoPageModels pages) {
        this.todoService = todoService;
        this.pages = pages;
    }

    @GetMapping("/plans/{id}/todos")
    public String list(@PathVariable("id") UUID planId, @ModelAttribute("filter") TodoListQuery filter, Model model) {
        return pages.list(model, planId, filter, new TodoForm());
    }

    /**
     * The add form carries the current list state as hidden fields (revision 1 Q10). Because TodoForm owns the
     * "priority" parameter, the list's priority filter arrives as hidden "listPriority" on this form only.
     */
    @PostMapping("/plans/{id}/todos")
    public String create(@PathVariable("id") UUID planId,
                         @Valid @ModelAttribute("todoForm") TodoForm form, BindingResult result,
                         @ModelAttribute("filter") TodoListQuery filter,
                         @RequestParam(name = "listPriority", required = false) String listPriority,
                         Model model, RedirectAttributes redirect, HttpServletResponse response) {
        filter.setPriority(listPriority);
        if (result.hasErrors()) {
            return pages.list(model, planId, filter, form);
        }
        try {
            todoService.create(planId, form.toCommand());
        } catch (DomainRuleException ex) {
            FormErrors.reject(result, ex);
            return pages.list(model, planId, filter, form);
        } catch (ConcurrencyConflictException ex) {
            ConflictResponses.rejectForm(result, response);
            return pages.list(model, planId, filter, form);
        }
        FlashMessages.add(redirect, FLASH_CREATED);
        return "redirect:" + filter.listUrl(planId);
    }

    @GetMapping("/todos/{id}")
    public String detail(@PathVariable("id") UUID todoId, Model model) {
        return pages.detail(model, todoId, new ExecutionForm());
    }

    @GetMapping("/todos/{id}/edit")
    public String editForm(@PathVariable("id") UUID todoId, Model model) {
        TodoRow todo = todoService.get(todoId);
        model.addAttribute("todoForm", TodoForm.from(todo));
        return editView(model, todo);
    }

    @PutMapping("/todos/{id}")
    public String update(@PathVariable("id") UUID todoId, @Valid @ModelAttribute("todoForm") TodoForm form,
                         BindingResult result, Model model, RedirectAttributes redirect,
                         HttpServletResponse response) {
        if (result.hasErrors()) {
            return editView(model, todoService.get(todoId));
        }
        try {
            todoService.update(todoId, form.toCommand());
        } catch (DomainRuleException ex) {
            FormErrors.reject(result, ex);
            return editView(model, todoService.get(todoId));
        } catch (ConcurrencyConflictException ex) {
            ConflictResponses.rejectForm(result, response);
            return editView(model, todoService.get(todoId));
        }
        FlashMessages.add(redirect, FLASH_UPDATED);
        return "redirect:/todos/" + todoId;
    }

    @DeleteMapping("/todos/{id}")
    public String delete(@PathVariable("id") UUID todoId, @ModelAttribute("filter") TodoListQuery filter,
                         RedirectAttributes redirect) {
        UUID planId;
        try {
            planId = todoService.delete(todoId);
        } catch (ConcurrencyConflictException ex) {
            // Nothing changed; back to the list the request came from (the plan id only builds the URL).
            FlashMessages.add(redirect, ConflictKeys.FLASH_RETRY);
            return "redirect:" + filter.listUrl(todoService.planIdForRedirect(todoId));
        }
        FlashMessages.add(redirect, FLASH_DELETED);
        return "redirect:" + filter.listUrl(planId);
    }

    private String editView(Model model, TodoRow todo) {
        model.addAttribute("todo", todo);
        model.addAttribute("priorities", Priority.values());
        return "todos/form";
    }
}
