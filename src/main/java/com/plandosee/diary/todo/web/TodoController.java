package com.plandosee.diary.todo.web;

import java.util.Optional;
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

import com.plandosee.diary.common.domain.EditOutcome;
import com.plandosee.diary.common.domain.Priority;
import com.plandosee.diary.common.domain.EditSnapshot;
import com.plandosee.diary.common.error.RetryLaterException;
import com.plandosee.diary.common.error.ServiceBusyException;
import com.plandosee.diary.common.web.EditConflicts;
import com.plandosee.diary.common.error.DomainRuleException;
import com.plandosee.diary.common.paging.PageRequest;
import com.plandosee.diary.common.web.ConflictKeys;
import com.plandosee.diary.common.web.ConflictResponses;
import com.plandosee.diary.common.web.FlashMessages;
import com.plandosee.diary.common.web.FormErrors;
import com.plandosee.diary.execution.web.ExecutionForm;
import com.plandosee.diary.todo.application.TodoDeletedException;
import com.plandosee.diary.todo.application.TodoService;
import com.plandosee.diary.todo.application.TodoStaleException;
import com.plandosee.diary.todo.domain.TodoRow;

/**
 * Todo list/create/edit/delete and todo detail.
 */
@Controller
public class TodoController {

    public static final String FLASH_CREATED = "flash.todo.created";
    public static final String FLASH_UPDATED = "flash.todo.updated";
    /** The saved content equals the stored todo; nothing changed and no revision was added. */
    public static final String FLASH_UNCHANGED = "flash.todo.unchanged";
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
     * The add form carries the current list state as hidden fields. Because TodoForm owns the
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
            return listAgain(model, planId, filter, form);
        } catch (ServiceBusyException ex) {
            // Out of time or connections. Shown again without reading the database, input kept (503).
            ConflictResponses.rejectForm(result, response, ex);
            return pages.listUnavailable(model, planId, filter, form);
        } catch (RetryLaterException ex) {
            ConflictResponses.rejectForm(result, response, ex);
            return listAgain(model, planId, filter, form);
        }
        FlashMessages.add(redirect, FLASH_CREATED);
        return "redirect:" + filter.listUrl(planId);
    }

    /** The list page after a failed add; without the database if even that read does not fit in the budget. */
    private String listAgain(Model model, UUID planId, TodoListQuery filter, TodoForm form) {
        return EditConflicts.readForForm(() -> pages.list(model, planId, filter, form))
                .orElseGet(() -> pages.listUnavailable(model, planId, filter, form));
    }

    @GetMapping("/todos/{id}")
    public String detail(@PathVariable("id") UUID todoId,
                         @RequestParam(name = "logPage", required = false) String logPage,
                         @RequestParam(name = "historyPage", required = false) String historyPage, Model model) {
        return pages.detail(model, todoId, new ExecutionForm(), PageRequest.parse(logPage),
                PageRequest.parse(historyPage));
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
        EditOutcome outcome;
        try {
            outcome = todoService.update(todoId, form.toCommand(), form.getVersion());
        } catch (TodoStaleException ex) {
            // 409, input kept, latest todo (with tags and status) and what differs; hidden version = latest.
            form.setVersion(ex.latestVersion());
            EditConflicts.rejectStale(result, response, model, ex, ex.latest());
            return editView(model, ex.latest());
        } catch (TodoDeletedException ex) {
            return pages.deleted(model, response, todoId, ex.planId(), TodoPageModels.DELETED_FROM_EDIT);
        } catch (DomainRuleException ex) {
            FormErrors.reject(result, ex);
            return editView(model, todoService.get(todoId));
        } catch (ServiceBusyException ex) {
            ConflictResponses.rejectForm(result, response, ex);
            EditConflicts.markSnapshotUnavailable(model);
            return editView(model, standIn(todoId, form));
        } catch (RetryLaterException ex) {
            ConflictResponses.rejectForm(result, response, ex);
            return editViewWithLatest(model, todoId, form);
        }
        FlashMessages.add(redirect, outcome == EditOutcome.UNCHANGED ? FLASH_UNCHANGED : FLASH_UPDATED);
        return "redirect:/todos/" + todoId;
    }

    @DeleteMapping("/todos/{id}")
    public String delete(@PathVariable("id") UUID todoId, @ModelAttribute("filter") TodoListQuery filter,
                         RedirectAttributes redirect) {
        UUID planId;
        try {
            planId = todoService.delete(todoId);
        } catch (TodoDeletedException ex) {
            // Already deleted (another tab): the result the user wanted; back to the list with a notice.
            FlashMessages.add(redirect, ConflictKeys.FLASH_TODO_ALREADY_DELETED);
            return "redirect:" + filter.listUrl(ex.planId());
        } catch (RetryLaterException ex) {
            // Nothing changed; back to the list the request came from (the plan id only builds the URL).
            FlashMessages.add(redirect, ConflictResponses.flashKey(ex, ConflictKeys.FLASH_RETRY));
            return "redirect:" + filter.listUrl(todoService.planIdForRedirect(todoId));
        }
        FlashMessages.add(redirect, FLASH_DELETED);
        return "redirect:" + filter.listUrl(planId);
    }

    /** After a collision: the latest todo and what differs, or the stand-in if even the read fails. */
    private String editViewWithLatest(Model model, UUID todoId, TodoForm form) {
        Optional<EditSnapshot<TodoRow>> read =
                EditConflicts.readForForm(() -> todoService.latestForEdit(todoId, form.toCommand()));
        if (read.isEmpty()) {
            EditConflicts.markSnapshotUnavailable(model);
            return editView(model, standIn(todoId, form));
        }
        EditSnapshot<TodoRow> snapshot = read.get();
        EditConflicts.showLatest(model, snapshot.latest(), snapshot.changedFields());
        return editView(model, snapshot.latest());
    }

    /**
     * The todo as far as it is known without the database: its id and the submitted title. planId is unknown (null);
     * the page says snapshotUnavailable.
     */
    private static TodoRow standIn(UUID todoId, TodoForm form) {
        TodoRow todo = new TodoRow();
        todo.setId(todoId);
        todo.setTitle(form.getTitle());
        todo.setVersion(form.getVersion() == null ? 0 : form.getVersion());
        return todo;
    }

    private String editView(Model model, TodoRow todo) {
        model.addAttribute("todo", todo);
        model.addAttribute("priorities", Priority.values());
        return "todos/form";
    }
}
