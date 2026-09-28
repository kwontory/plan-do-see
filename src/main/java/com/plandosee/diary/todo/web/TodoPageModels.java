package com.plandosee.diary.todo.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import com.plandosee.diary.common.domain.Priority;
import com.plandosee.diary.common.id.IdGenerator;
import com.plandosee.diary.execution.application.ExecutionService;
import com.plandosee.diary.execution.application.TodoExecutionLogs;
import com.plandosee.diary.execution.web.ExecutionForm;
import com.plandosee.diary.plan.application.PlanService;
import com.plandosee.diary.plan.domain.PlanRow;
import com.plandosee.diary.todo.application.TodoService;
import com.plandosee.diary.todo.domain.DueFilter;
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
        List<TodoRow> todos = todoService.search(planId, filter.getQ(), filter.statusValue(), filter.priorityValue(),
                filter.tagIdValue(), filter.dueValue(), sort);
        Map<UUID, UUID> completionKeys = new LinkedHashMap<>();
        todos.forEach(todo -> completionKeys.put(todo.getId(), idGenerator.newId()));

        model.addAttribute("plan", plan);
        model.addAttribute("todos", todos);
        model.addAttribute("filter", filter);
        model.addAttribute("sorts", TodoSort.values());
        model.addAttribute("statuses", TodoStatus.values());
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("dueFilters", DueFilter.values());
        model.addAttribute("tags", todoService.tags());
        model.addAttribute("todoForm", todoForm);
        model.addAttribute("completionKeys", completionKeys);
        return "todos/list";
    }

    public String detail(Model model, UUID todoId, ExecutionForm executionForm) {
        TodoRow todo = todoService.get(todoId);
        model.addAttribute("todo", todo);
        model.addAttribute("plan", planService.get(todo.getPlanId()));
        TodoExecutionLogs logs = executionService.logsForTodo(todoId);
        model.addAttribute("logs", logs.logs());
        model.addAttribute("logsActualMinutes", logs.actualMinutes());
        model.addAttribute("events", todoService.completionEvents(todoId));
        model.addAttribute("executionForm", executionForm);
        model.addAttribute("completionKey", idGenerator.newId());
        return "todos/detail";
    }
}
