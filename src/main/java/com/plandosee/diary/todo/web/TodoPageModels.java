package com.plandosee.diary.todo.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import com.plandosee.diary.common.domain.Priority;
import com.plandosee.diary.common.error.DomainRuleException;
import com.plandosee.diary.common.error.FieldViolation;
import com.plandosee.diary.common.id.IdGenerator;
import com.plandosee.diary.common.paging.Page;
import com.plandosee.diary.common.paging.PageInfo;
import com.plandosee.diary.common.paging.PageRequest;
import com.plandosee.diary.common.paging.PageSettings;
import com.plandosee.diary.common.web.EditConflicts;
import com.plandosee.diary.common.web.FormErrors;
import com.plandosee.diary.common.web.PageNavigation;
import com.plandosee.diary.execution.application.ExecutionService;
import com.plandosee.diary.execution.application.TodoExecutionLogs;
import com.plandosee.diary.execution.web.ExecutionForm;
import com.plandosee.diary.plan.application.PlanService;
import com.plandosee.diary.plan.domain.PlanRow;
import com.plandosee.diary.todo.application.TodoService;
import com.plandosee.diary.todo.domain.DueFilter;
import com.plandosee.diary.todo.domain.TagRow;
import com.plandosee.diary.todo.domain.TodoRow;
import com.plandosee.diary.todo.domain.TodoRules;
import com.plandosee.diary.todo.domain.TodoSort;
import com.plandosee.diary.todo.domain.TodoStatus;

/**
 * Builds the todo list and todo detail models so a failed POST re-renders exactly what the GET shows.
 * Completion idempotency keys are issued here, once per rendering.
 */
@Component
public class TodoPageModels {

    private final TodoService todoService;
    private final PlanService planService;
    private final ExecutionService executionService;
    private final IdGenerator idGenerator;
    private final PageSettings pageSettings;

    /** View for a todo that was deleted while a form for it was being submitted. */
    public static final String DELETED_VIEW = "todos/deleted";
    public static final String DELETED_FROM_EDIT = "edit";
    public static final String DELETED_FROM_EXECUTION = "execution";

    public TodoPageModels(TodoService todoService, PlanService planService, ExecutionService executionService,
                          IdGenerator idGenerator, PageSettings pageSettings) {
        this.todoService = todoService;
        this.planService = planService;
        this.executionService = executionService;
        this.idGenerator = idGenerator;
        this.pageSettings = pageSettings;
    }

    /**
     * Todo list. A search text the rule rejects (longer than TodoRules.SEARCH_QUERY_MAX, line break,
     * control character) is not searched: searchRejected is true, the filter's BindingResult has the field error on
     * "q" (code and args of the rule), filter keeps the text and every other filter, sort and requested page as sent,
     * and the result part is empty (todos empty, page and pageNav of an empty list, completionKeys empty) for the
     * template to leave out. Status 200. Otherwise searchRejected is false.
     */
    public String list(Model model, UUID planId, TodoListQuery rawFilter, TodoForm todoForm) {
        PlanRow plan = planService.get(planId);
        TodoListQuery filter = rawFilter == null ? new TodoListQuery().normalized() : rawFilter.normalized();
        TodoSort sort = filter.sortValue();
        List<FieldViolation> searchViolations = filter.searchViolations();
        DomainRuleException rejected = searchViolations.isEmpty() ? null : new DomainRuleException(searchViolations);
        Page<TodoRow> page = null;
        if (rejected == null) {
            try {
                page = todoService.searchPage(planId, filter.getQ(), filter.statusValue(), filter.priorityValue(),
                        filter.tagIdValue(), filter.dueValue(), sort, filter.pageValue());
                // The page actually shown (a page past the end is the last page), so hidden fields and links carry it.
                filter.setPage(String.valueOf(page.info().number()));
            } catch (DomainRuleException ex) {
                // The service's list command applies the same search rule; shown the same way.
                rejected = ex;
            }
        }
        PageInfo pageInfo = page == null ? pageSettings.page(PageRequest.FIRST, 0) : page.info();
        List<TodoRow> todos = page == null ? List.of() : page.items();
        Map<UUID, UUID> completionKeys = new LinkedHashMap<>();
        todos.forEach(todo -> completionKeys.put(todo.getId(), idGenerator.newId()));
        // Every read happens before the model is filled, so a failed read leaves no partial page behind.
        List<TagRow> tags = todoService.tags();

        model.addAttribute("plan", plan);
        model.addAttribute("todos", todos);
        PageNavigation.addTo(model, "page", pageInfo);
        model.addAttribute("filter", filter);
        // Added after "filter": replacing the attribute drops the BindingResult bound to the request's object.
        BindingResult filterResult = new BeanPropertyBindingResult(filter, "filter");
        if (rejected != null) {
            FormErrors.reject(filterResult, rejected);
        }
        model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "filter", filterResult);
        model.addAttribute("searchRejected", rejected != null);
        // Conditions in use for the list heading (ADR-41); a rejected search text was not applied.
        model.addAttribute("filterActiveCount", filter.activeFilterCount(rejected == null));
        // Longest search text the server accepts; for the search box maxlength and its help text.
        model.addAttribute("searchQueryMax", TodoRules.SEARCH_QUERY_MAX);
        model.addAttribute("sorts", TodoSort.values());
        model.addAttribute("statuses", TodoStatus.values());
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("dueFilters", DueFilter.values());
        model.addAttribute("tags", tags);
        model.addAttribute("todoForm", todoForm);
        model.addAttribute("completionKeys", completionKeys);
        return "todos/list";
    }

    /** Todo detail after a form POST: the first page of each list. */
    public String detail(Model model, UUID todoId, ExecutionForm executionForm) {
        return detail(model, todoId, executionForm, PageRequest.FIRST);
    }

    /**
     * HTTP 409 and the todos/deleted view. Nothing was stored. The submitted form stays in the model under
     * its usual name (todoForm or executionForm) so the page can show the input read-only for copying. Model:
     * todoId, planId (the plan the todo belonged to, for the list link), deletedFrom ("edit" or "execution").
     * No database read.
     */
    public String deleted(Model model, jakarta.servlet.http.HttpServletResponse response, UUID todoId, UUID planId,
                          String from) {
        response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_CONFLICT);
        model.addAttribute("todoId", todoId);
        model.addAttribute("planId", planId);
        model.addAttribute("deletedFrom", from);
        return DELETED_VIEW;
    }

    /**
     * Todo detail. logs is one page (query parameter logPage, model logPage and logPageNav); logsActualMinutes is the
     * total of all the todo's records (logsActualDuration: the same value as days, hours and minutes). Edit and
     * completion histories are not on the screen (ADR-43); they are stored and exported only.
     */
    public String detail(Model model, UUID todoId, ExecutionForm executionForm, int logPage) {
        // Every read happens before the model is filled, so a failed read leaves no partial page behind.
        TodoRow todo = todoService.get(todoId);
        PlanRow plan = planService.get(todo.getPlanId());
        TodoExecutionLogs logs = executionService.logsForTodo(todoId, logPage);
        model.addAttribute("todo", todo);
        model.addAttribute("plan", plan);
        model.addAttribute("logs", logs.logs());
        PageNavigation.addTo(model, "logPage", logs.page());
        model.addAttribute("logsActualMinutes", logs.actualMinutes());
        model.addAttribute("logsActualDuration", logs.actualDuration());
        model.addAttribute("executionForm", executionForm);
        model.addAttribute("completionKey", idGenerator.newId());
        return "todos/detail";
    }

    /**
     * Todo list shown again after the add form ran out of its time budget or pool wait, without reading the
     * database (a read would wait and fail again). Only what the request itself carries is in the model: plan (a
     * stand-in holding the id from the path; title and the rest unknown), filter (the submitted list state),
     * todoForm (the input), and the enum choices. todos, page, pageNav, tags and completionKeys are absent (null);
     * snapshotUnavailable is true. The caller has set the status and the global error.
     */
    public String listUnavailable(Model model, UUID planId, TodoListQuery rawFilter, TodoForm todoForm) {
        TodoListQuery filter = rawFilter == null ? new TodoListQuery().normalized() : rawFilter.normalized();
        PlanRow plan = new PlanRow();
        plan.setId(planId);
        EditConflicts.markSnapshotUnavailable(model);
        model.addAttribute("plan", plan);
        model.addAttribute("filter", filter);
        model.addAttribute("filterActiveCount", filter.activeFilterCount(filter.searchViolations().isEmpty()));
        model.addAttribute("sorts", TodoSort.values());
        model.addAttribute("statuses", TodoStatus.values());
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("dueFilters", DueFilter.values());
        model.addAttribute("todoForm", todoForm);
        return "todos/list";
    }

    /**
     * Todo detail shown again after the execution form ran out of its time budget or pool wait, without
     * reading the database. Model: todo (a stand-in holding the id from the path; title, planId and the rest
     * unknown), executionForm (the input), snapshotUnavailable = true. plan, logs, logPage, logPageNav,
     * logsActualMinutes, logsActualDuration and completionKey are absent (null). The caller has set the status and the global error.
     */
    public String detailUnavailable(Model model, UUID todoId, ExecutionForm executionForm) {
        TodoRow todo = new TodoRow();
        todo.setId(todoId);
        EditConflicts.markSnapshotUnavailable(model);
        model.addAttribute("todo", todo);
        model.addAttribute("executionForm", executionForm);
        return "todos/detail";
    }
}
