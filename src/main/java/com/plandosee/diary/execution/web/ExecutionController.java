package com.plandosee.diary.execution.web;

import java.util.UUID;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.plandosee.diary.common.error.RetryLaterException;
import com.plandosee.diary.common.error.DomainRuleException;
import com.plandosee.diary.common.error.ServiceBusyException;
import com.plandosee.diary.common.web.EditConflicts;
import com.plandosee.diary.common.web.ConflictResponses;
import com.plandosee.diary.common.web.FlashMessages;
import com.plandosee.diary.common.web.FormErrors;
import com.plandosee.diary.common.domain.EditOutcome;
import com.plandosee.diary.common.domain.EditSnapshot;
import com.plandosee.diary.execution.application.ExecutionEditResult;
import com.plandosee.diary.execution.application.ExecutionService;
import com.plandosee.diary.execution.application.ExecutionStaleException;
import com.plandosee.diary.execution.domain.ExecutionLogRow;
import com.plandosee.diary.todo.application.TodoDeletedException;
import com.plandosee.diary.todo.web.TodoPageModels;

/**
 * Execution record on the todo detail page. A rejected range (end before start) re-renders the detail page with a field error.
 * A todo deleted by a concurrent request shows todos/deleted with the input read-only (409); a save that
 * kept colliding with other requests re-renders the form with the input kept and HTTP 409; a save that ran out
 * of its time budget or pool wait re-renders it with HTTP 503 without reading the database (
 * snapshotUnavailable).
 * <p>
 * Editing (ADR-40), like the plan and todo edit forms: GET /executions/{id}/edit shows {@value #EDIT_VIEW} with
 * {@code log} (the record with todoId, todoTitle, version) and {@code executionForm} (startedAt, endedAt,
 * blockerReason, hidden version); PUT /executions/{id} (POST + _method=put) saves and redirects to the todo page with
 * {@link #FLASH_UPDATED} or {@link #FLASH_UNCHANGED}. A rule violation or an overlap re-renders the form (200) with the
 * field errors; an out-of-date form is 409 with {@code latest} and {@code changedFields}; a collision 409 or busy 503
 * keeps the input.
 */
@Controller
public class ExecutionController {

    public static final String FLASH_RECORDED = "flash.execution.recorded";
    /** An execution record was edited (the previous values are kept, not shown). */
    public static final String FLASH_UPDATED = "flash.execution.updated";
    /** The edit equals the stored record; nothing was written. */
    public static final String FLASH_UNCHANGED = "flash.execution.unchanged";
    static final String EDIT_VIEW = "executions/form";

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
            // The input is shown read-only on todos/deleted (409) instead of being dropped by a redirect.
            return pages.deleted(model, response, todoId, ex.planId(), TodoPageModels.DELETED_FROM_EXECUTION);
        } catch (DomainRuleException ex) {
            FormErrors.reject(result, ex);
            return detailAgain(model, todoId, form);
        } catch (ServiceBusyException ex) {
            // Out of time or connections. Shown again without reading the database, input kept (503).
            ConflictResponses.rejectForm(result, response, ex);
            return pages.detailUnavailable(model, todoId, form);
        } catch (RetryLaterException ex) {
            ConflictResponses.rejectForm(result, response, ex);
            return detailAgain(model, todoId, form);
        }
        FlashMessages.add(redirect, FLASH_RECORDED);
        return "redirect:/todos/" + todoId;
    }

    /** The detail page after a failed save; without the database if even that read does not fit in the budget. */
    private String detailAgain(Model model, UUID todoId, ExecutionForm form) {
        return EditConflicts.readForForm(() -> pages.detail(model, todoId, form))
                .orElseGet(() -> pages.detailUnavailable(model, todoId, form));
    }

    @GetMapping("/executions/{id}/edit")
    public String editForm(@PathVariable("id") UUID logId, Model model) {
        ExecutionLogRow log = executionService.get(logId);
        model.addAttribute("executionForm", ExecutionForm.from(log));
        return editView(model, log);
    }

    @PutMapping("/executions/{id}")
    public String update(@PathVariable("id") UUID logId, @Valid @ModelAttribute("executionForm") ExecutionForm form,
                         BindingResult result, Model model, RedirectAttributes redirect,
                         HttpServletResponse response) {
        if (result.hasErrors()) {
            return editView(model, executionService.get(logId));
        }
        ExecutionEditResult saved;
        try {
            saved = executionService.revise(logId, form.toCommand(), form.getVersion());
        } catch (ExecutionStaleException ex) {
            form.setVersion(ex.latestVersion());
            EditConflicts.rejectStale(result, response, model, ex, ex.latest());
            return editView(model, ex.latest());
        } catch (DomainRuleException ex) {
            FormErrors.reject(result, ex);
            return editViewWithLatest(model, logId, form, false);
        } catch (ServiceBusyException ex) {
            ConflictResponses.rejectForm(result, response, ex);
            EditConflicts.markSnapshotUnavailable(model);
            return editView(model, standIn(logId, form));
        } catch (RetryLaterException ex) {
            ConflictResponses.rejectForm(result, response, ex);
            return editViewWithLatest(model, logId, form, true);
        }
        FlashMessages.add(redirect, saved.outcome() == EditOutcome.UNCHANGED ? FLASH_UNCHANGED : FLASH_UPDATED);
        return "redirect:/todos/" + saved.todoId();
    }

    /**
     * The form again after a failed save: the stored record for the page (and, after a collision, what differs), or
     * the stand-in when even that read does not fit in the budget.
     */
    private String editViewWithLatest(Model model, UUID logId, ExecutionForm form, boolean showLatest) {
        java.util.Optional<EditSnapshot<ExecutionLogRow>> read = EditConflicts.readForForm(() -> {
            ExecutionLogRow latest = executionService.get(logId);
            return showLatest ? executionService.latestForEdit(logId, form.toCommand())
                    : new EditSnapshot<>(latest, java.util.List.of());
        });
        if (read.isEmpty()) {
            EditConflicts.markSnapshotUnavailable(model);
            return editView(model, standIn(logId, form));
        }
        if (showLatest) {
            EditConflicts.showLatest(model, read.get().latest(), read.get().changedFields());
        }
        return editView(model, read.get().latest());
    }

    /** The record as far as it is known without the database: its id and the submitted values. */
    private static ExecutionLogRow standIn(UUID logId, ExecutionForm form) {
        ExecutionLogRow log = new ExecutionLogRow();
        log.setId(logId);
        log.setStartedAt(form.startedAtInSeoul());
        log.setEndedAt(form.endedAtInSeoul());
        log.setBlockerReason(form.getBlockerReason());
        log.setVersion(form.getVersion() == null ? 0 : form.getVersion());
        return log;
    }

    private static String editView(Model model, ExecutionLogRow log) {
        model.addAttribute("log", log);
        return EDIT_VIEW;
    }
}
