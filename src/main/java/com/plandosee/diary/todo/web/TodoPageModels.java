package com.plandosee.diary.todo.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import com.plandosee.diary.common.domain.Priority;
import com.plandosee.diary.common.id.IdGenerator;
import com.plandosee.diary.common.paging.Page;
import com.plandosee.diary.common.paging.PageRequest;
import com.plandosee.diary.common.web.EditConflicts;
import com.plandosee.diary.common.web.PageNavigation;
import com.plandosee.diary.execution.application.ExecutionService;
import com.plandosee.diary.execution.application.TodoExecutionLogs;
import com.plandosee.diary.execution.web.ExecutionForm;
import com.plandosee.diary.plan.application.PlanService;
import com.plandosee.diary.plan.domain.PlanRow;
import com.plandosee.diary.todo.application.CompletionHistory;
import com.plandosee.diary.todo.application.TodoService;
import com.plandosee.diary.todo.domain.DueFilter;
import com.plandosee.diary.todo.domain.TagRow;
import com.plandosee.diary.todo.domain.TodoRevisionRow;
import com.plandosee.diary.todo.domain.TodoRow;
import com.plandosee.diary.todo.domain.TodoSort;
import com.plandosee.diary.todo.domain.TodoStatus;

/**
 * Builds the S02 list and S03 detail models so a failed POST re-renders exactly what the GET shows.
 * Completion idempotency keys are issued here, once per rendering (ADR-05).
 */
@Component
public class TodoPageModels {

    private final TodoService todoService;
    private final PlanService planService;
    private final ExecutionService executionService;
    private final IdGenerator idGenerator;

    /** ADR-18 E8 view: the todo was deleted while a form for it was being submitted (template owned by Frontend). */
    public static final String DELETED_VIEW = "todos/deleted";
    public static final String DELETED_FROM_EDIT = "edit";
    public static final String DELETED_FROM_EXECUTION = "execution";

    public TodoPageModels(TodoService todoService, PlanService planService, ExecutionService executionService,
                          IdGenerator idGenerator) {
        this.todoService = todoService;
        this.planService = planService;
        this.executionService = executionService;
        this.idGenerator = idGenerator;
    }

    public String list(Model model, UUID planId, TodoListQuery rawFilter, TodoForm todoForm) {
        PlanRow plan = planService.get(planId);
        TodoListQuery filter = rawFilter == null ? new TodoListQuery().normalized() : rawFilter.normalized();
        TodoSort sort = filter.sortValue();
        Page<TodoRow> page = todoService.searchPage(planId, filter.getQ(), filter.statusValue(), filter.priorityValue(),
                filter.tagIdValue(), filter.dueValue(), sort, filter.pageValue());
        List<TodoRow> todos = page.items();
        // The page actually shown (a page past the end is the last page), so hidden fields and links carry it.
        filter.setPage(String.valueOf(page.info().number()));
        Map<UUID, UUID> completionKeys = new LinkedHashMap<>();
        todos.forEach(todo -> completionKeys.put(todo.getId(), idGenerator.newId()));
        // Every read happens before the model is filled, so a failed read leaves no partial page behind.
        List<TagRow> tags = todoService.tags();

        model.addAttribute("plan", plan);
        model.addAttribute("todos", todos);
        PageNavigation.addTo(model, "page", page.info());
        model.addAttribute("filter", filter);
        model.addAttribute("sorts", TodoSort.values());
        model.addAttribute("statuses", TodoStatus.values());
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("dueFilters", DueFilter.values());
        model.addAttribute("tags", tags);
        model.addAttribute("todoForm", todoForm);
        model.addAttribute("completionKeys", completionKeys);
        return "todos/list";
    }

    /** S03 after a form POST: the first page of each list. */
    public String detail(Model model, UUID todoId, ExecutionForm executionForm) {
        return detail(model, todoId, executionForm, PageRequest.FIRST, PageRequest.FIRST);
    }

    /**
     * ADR-18 E8: HTTP 409 and the todos/deleted view. Nothing was stored. The submitted form stays in the model under
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
     * S03. logs is one page (ADR-21, query parameter logPage, model logPage and logPageNav); logsActualMinutes is the
     * total of all the todo's records. ADR-16: todoRevisions (newest first), completionHistory (one page of
     * completions and reopens merged in time order, query parameter historyPage, model historyPage and
     * historyPageNav), completionCount, reopenCount (all events).
     */
    public String detail(Model model, UUID todoId, ExecutionForm executionForm, int logPage, int historyPage) {
        // Every read happens before the model is filled, so a failed read leaves no partial page behind.
        TodoRow todo = todoService.get(todoId);
        PlanRow plan = planService.get(todo.getPlanId());
        TodoExecutionLogs logs = executionService.logsForTodo(todoId, logPage);
        CompletionHistory history = todoService.completionHistory(todoId, historyPage);
        List<TodoRevisionRow> revisions = todoService.revisions(todoId);
        model.addAttribute("todo", todo);
        model.addAttribute("plan", plan);
        model.addAttribute("logs", logs.logs());
        PageNavigation.addTo(model, "logPage", logs.page());
        model.addAttribute("logsActualMinutes", logs.actualMinutes());
        model.addAttribute("completionHistory", history.entries());
        PageNavigation.addTo(model, "historyPage", history.page());
        model.addAttribute("completionCount", history.completionCount());
        model.addAttribute("reopenCount", history.reopenCount());
        model.addAttribute("todoRevisions", revisions);
        model.addAttribute("executionForm", executionForm);
        model.addAttribute("completionKey", idGenerator.newId());
        return "todos/detail";
    }

    /**
     * ADR-15/ADR-19: S02 shown again after the add form ran out of its time budget or pool wait, without reading the
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
        model.addAttribute("sorts", TodoSort.values());
        model.addAttribute("statuses", TodoStatus.values());
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("dueFilters", DueFilter.values());
        model.addAttribute("todoForm", todoForm);
        return "todos/list";
    }

    /**
     * ADR-15/ADR-19: S03 shown again after the execution form ran out of its time budget or pool wait, without
     * reading the database. Model: todo (a stand-in holding the id from the path; title, planId and the rest
     * unknown), executionForm (the input), snapshotUnavailable = true. plan, logs, logPage, logPageNav,
     * logsActualMinutes, completionHistory, historyPage, historyPageNav, completionCount, reopenCount, todoRevisions
     * and completionKey are absent (null). The caller has set the status and the global error.
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
