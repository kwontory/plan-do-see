package com.plandosee.diary.todo.application;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plandosee.diary.common.config.CurrentUserProvider;
import com.plandosee.diary.common.domain.Priority;
import com.plandosee.diary.common.error.NotFoundException;
import com.plandosee.diary.common.id.IdGenerator;
import com.plandosee.diary.common.time.SeoulDates;
import com.plandosee.diary.plan.infrastructure.PlanMapper;
import com.plandosee.diary.todo.domain.CompletionEventRow;
import com.plandosee.diary.todo.domain.DueFilter;
import com.plandosee.diary.todo.domain.TagRow;
import com.plandosee.diary.todo.domain.TodoFilter;
import com.plandosee.diary.todo.domain.TodoRow;
import com.plandosee.diary.todo.domain.TodoSort;
import com.plandosee.diary.todo.domain.TodoStatus;
import com.plandosee.diary.todo.infrastructure.TagMapper;
import com.plandosee.diary.todo.infrastructure.TodoMapper;

@Service
public class TodoService {

    private final TodoMapper todoMapper;
    private final TagMapper tagMapper;
    private final PlanMapper planMapper;
    private final CurrentUserProvider currentUserProvider;
    private final IdGenerator idGenerator;
    private final SeoulDates seoulDates;

    public TodoService(TodoMapper todoMapper, TagMapper tagMapper, PlanMapper planMapper,
                       CurrentUserProvider currentUserProvider, IdGenerator idGenerator, SeoulDates seoulDates) {
        this.todoMapper = todoMapper;
        this.tagMapper = tagMapper;
        this.planMapper = planMapper;
        this.currentUserProvider = currentUserProvider;
        this.idGenerator = idGenerator;
        this.seoulDates = seoulDates;
    }

    @Transactional
    public UUID create(UUID planId, TodoCommand command) {
        UUID userId = currentUserProvider.currentUserId();
        if (planMapper.findActiveOwned(userId, planId) == null) {
            throw new NotFoundException("plan");
        }
        OffsetDateTime now = now();
        TodoRow todo = new TodoRow();
        todo.setId(idGenerator.newId());
        todo.setPlanId(planId);
        applyContent(todo, command);
        todo.setStatus(TodoStatus.IN_PROGRESS);
        todo.setCompletionCycle(0);
        todo.setCompletedAt(null);
        todo.setCreatedAt(now);
        todo.setUpdatedAt(now);
        todoMapper.insert(todo);
        replaceTags(userId, todo.getId(), command.tagNames(), now);
        return todo.getId();
    }

    /**
     * Only title, due date, priority, estimate, and tags change (ADR-07). Status, plan, and id are untouched.
     */
    @Transactional
    public void update(UUID todoId, TodoCommand command) {
        UUID userId = currentUserProvider.currentUserId();
        TodoRow todo = todoMapper.lockActiveOwned(userId, todoId);
        if (todo == null) {
            throw new NotFoundException("todo");
        }
        OffsetDateTime now = now();
        applyContent(todo, command);
        todo.setUpdatedAt(now);
        if (todoMapper.updateContentOwned(userId, todo) != 1) {
            throw new NotFoundException("todo");
        }
        replaceTags(userId, todoId, command.tagNames(), now);
    }

    @Transactional
    public UUID delete(UUID todoId) {
        UUID userId = currentUserProvider.currentUserId();
        TodoRow todo = todoMapper.lockActiveOwned(userId, todoId);
        if (todo == null) {
            throw new NotFoundException("todo");
        }
        todoMapper.softDeleteOwned(userId, todoId, now());
        return todo.getPlanId();
    }

    @Transactional(readOnly = true)
    public TodoRow get(UUID todoId) {
        UUID userId = currentUserProvider.currentUserId();
        TodoRow todo = todoMapper.findActiveOwned(userId, todoId);
        if (todo == null) {
            throw new NotFoundException("todo");
        }
        attachTags(userId, List.of(todo));
        return todo;
    }

    @Transactional(readOnly = true)
    public List<TodoRow> search(UUID planId, String query, TodoStatus status, Priority priority,
                                UUID tagId, DueFilter due, TodoSort sort) {
        UUID userId = currentUserProvider.currentUserId();
        if (planMapper.findActiveOwned(userId, planId) == null) {
            throw new NotFoundException("plan");
        }
        TodoFilter filter = new TodoFilter(userId, planId, query, status, priority, tagId, due, sort, seoulDates.today());
        List<TodoRow> todos = todoMapper.search(filter);
        attachTags(userId, todos);
        return todos;
    }

    @Transactional(readOnly = true)
    public List<CompletionEventRow> completionEvents(UUID todoId) {
        UUID userId = currentUserProvider.currentUserId();
        if (todoMapper.findActiveOwned(userId, todoId) == null) {
            throw new NotFoundException("todo");
        }
        return todoMapper.listCompletionEventsOwned(userId, todoId);
    }

    @Transactional(readOnly = true)
    public List<TagRow> tags() {
        return tagMapper.listActiveOwned(currentUserProvider.currentUserId());
    }

    private void applyContent(TodoRow todo, TodoCommand command) {
        todo.setTitle(command.title().strip());
        todo.setDueDate(command.dueDate());
        todo.setPriority(command.priority());
        todo.setEstimatedMinutes(command.estimatedMinutes());
    }

    private void replaceTags(UUID userId, UUID todoId, List<String> tagNames, OffsetDateTime now) {
        tagMapper.deleteTodoLinks(todoId);
        for (String name : tagNames) {
            tagMapper.insertIfAbsent(idGenerator.newId(), userId, name, now);
            UUID tagId = tagMapper.findActiveIdByName(userId, name);
            tagMapper.insertTodoLink(todoId, tagId, now);
        }
    }

    private void attachTags(UUID userId, List<TodoRow> todos) {
        if (todos.isEmpty()) {
            return;
        }
        Map<UUID, TodoRow> byId = new LinkedHashMap<>();
        for (TodoRow todo : todos) {
            todo.setTags(new ArrayList<>());
            byId.put(todo.getId(), todo);
        }
        for (TagRow tag : tagMapper.listForTodos(userId, new ArrayList<>(byId.keySet()))) {
            byId.get(tag.getTodoId()).getTags().add(tag);
        }
    }

    private OffsetDateTime now() {
        return seoulDates.now().atOffset(ZoneOffset.UTC);
    }
}
