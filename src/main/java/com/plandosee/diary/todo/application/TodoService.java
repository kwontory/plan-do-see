package com.plandosee.diary.todo.application;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.plandosee.diary.common.concurrency.WriteTransactions;
import com.plandosee.diary.common.config.CurrentUserProvider;
import com.plandosee.diary.common.domain.Priority;
import com.plandosee.diary.common.error.NotFoundException;
import com.plandosee.diary.common.id.IdGenerator;
import com.plandosee.diary.common.time.SeoulDates;
import com.plandosee.diary.plan.application.PlanService;
import com.plandosee.diary.todo.application.port.TagMapper;
import com.plandosee.diary.todo.application.port.TodoMapper;
import com.plandosee.diary.todo.domain.CompletionEventRow;
import com.plandosee.diary.todo.domain.DueFilter;
import com.plandosee.diary.todo.domain.TagRow;
import com.plandosee.diary.todo.domain.TodoFilter;
import com.plandosee.diary.todo.domain.TodoRow;
import com.plandosee.diary.todo.domain.TodoSort;
import com.plandosee.diary.todo.domain.TodoStatus;

@Service
public class TodoService {

    private final TodoMapper todoMapper;
    private final TagMapper tagMapper;
    private final PlanService planService;
    private final CurrentUserProvider currentUserProvider;
    private final IdGenerator idGenerator;
    private final SeoulDates seoulDates;
    private final WriteTransactions writes;

    public TodoService(TodoMapper todoMapper, TagMapper tagMapper, PlanService planService,
                       CurrentUserProvider currentUserProvider, IdGenerator idGenerator, SeoulDates seoulDates,
                       WriteTransactions writes) {
        this.todoMapper = todoMapper;
        this.tagMapper = tagMapper;
        this.planService = planService;
        this.currentUserProvider = currentUserProvider;
        this.idGenerator = idGenerator;
        this.seoulDates = seoulDates;
        this.writes = writes;
    }

    public UUID create(UUID planId, TodoCommand command) {
        return writes.run(() -> {
            UUID userId = currentUserProvider.currentUserId();
            planService.requireOwned(planId);
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
        });
    }

    /**
     * Only title, due date, priority, estimate, and tags change (ADR-07). Status, plan, and id are untouched.
     */
    public void update(UUID todoId, TodoCommand command) {
        writes.run(() -> {
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
        });
    }

    public UUID delete(UUID todoId) {
        return writes.run(() -> {
            UUID userId = currentUserProvider.currentUserId();
            TodoRow todo = todoMapper.lockActiveOwned(userId, todoId);
            if (todo == null) {
                throw new NotFoundException("todo");
            }
            todoMapper.softDeleteOwned(userId, todoId, now());
            return todo.getPlanId();
        });
    }

    /**
     * Locks the owned active todo in the caller's transaction (ADR-14): completion and execution records take this
     * lock so they serialize with each other and with a concurrent delete.
     * <ul>
     *   <li>Not active when the request started (already deleted, never existed, not owned): NotFoundException
     *       (404, T06-C13).</li>
     *   <li>Active when the request started but deleted by a concurrent request before the lock was granted:
     *       {@link TodoDeletedException} (ADR-15), so the caller can send the user to the list.</li>
     * </ul>
     * The first read is a plain read; under READ COMMITTED the lock statement takes a newer snapshot and re-checks
     * the row after any lock wait, which is where a concurrent delete becomes visible.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public TodoRow lockOwned(UUID todoId) {
        UUID userId = currentUserProvider.currentUserId();
        if (todoMapper.findActiveOwned(userId, todoId, seoulDates.today()) == null) {
            throw new NotFoundException("todo");
        }
        TodoRow todo = todoMapper.lockActiveOwned(userId, todoId);
        if (todo == null) {
            throw deletedMeanwhile(userId, todoId);
        }
        return todo;
    }

    /**
     * The owned active todo without tags, for ownership checks by other features. NotFoundException otherwise.
     */
    @Transactional(readOnly = true)
    public TodoRow requireOwned(UUID todoId) {
        UUID userId = currentUserProvider.currentUserId();
        TodoRow todo = todoMapper.findActiveOwned(userId, todoId, seoulDates.today());
        if (todo == null) {
            throw new NotFoundException("todo");
        }
        return todo;
    }

    /**
     * Plan of an owned todo, deleted or not, only to choose a redirect after a failed request (never to decide a
     * rule). NotFoundException when it never existed or is not owned.
     */
    @Transactional(readOnly = true)
    public UUID planIdForRedirect(UUID todoId) {
        UUID planId = todoMapper.findPlanIdOwnedIncludingDeleted(currentUserProvider.currentUserId(), todoId);
        if (planId == null) {
            throw new NotFoundException("todo");
        }
        return planId;
    }

    @Transactional(readOnly = true)
    public TodoRow get(UUID todoId) {
        UUID userId = currentUserProvider.currentUserId();
        TodoRow todo = todoMapper.findActiveOwned(userId, todoId, seoulDates.today());
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
        planService.requireOwned(planId);
        TodoFilter filter = new TodoFilter(userId, planId, query, status, priority, tagId, due, sort, seoulDates.today());
        List<TodoRow> todos = todoMapper.search(filter);
        attachTags(userId, todos);
        return todos;
    }

    @Transactional(readOnly = true)
    public List<CompletionEventRow> completionEvents(UUID todoId) {
        UUID userId = currentUserProvider.currentUserId();
        if (todoMapper.findActiveOwned(userId, todoId, seoulDates.today()) == null) {
            throw new NotFoundException("todo");
        }
        return todoMapper.listCompletionEventsOwned(userId, todoId);
    }

    @Transactional(readOnly = true)
    public List<TagRow> tags() {
        return tagMapper.listActiveOwned(currentUserProvider.currentUserId());
    }

    private NotFoundException deletedMeanwhile(UUID userId, UUID todoId) {
        UUID planId = todoMapper.findPlanIdOwnedIncludingDeleted(userId, todoId);
        return planId == null ? new NotFoundException("todo") : new TodoDeletedException(planId);
    }

    private void applyContent(TodoRow todo, TodoCommand command) {
        todo.setTitle(command.title().strip());
        todo.setDueDate(command.dueDate());
        todo.setPriority(command.priority());
        todo.setEstimatedMinutes(command.estimatedMinutes());
    }

    /**
     * New tag rows hold the unique-index entry of their normalized name until commit. Inserting in the input order
     * let two saves with the same new tags in opposite order deadlock (ADR-15 CC-1); every transaction now inserts
     * in normalized-name order, so they queue instead.
     */
    private void replaceTags(UUID userId, UUID todoId, List<String> tagNames, OffsetDateTime now) {
        tagMapper.deleteTodoLinks(todoId);
        for (String name : inLockOrder(tagNames)) {
            tagMapper.insertIfAbsent(idGenerator.newId(), userId, name, now);
            UUID tagId = tagMapper.findActiveIdByName(userId, name);
            tagMapper.insertTodoLink(todoId, tagId, now);
        }
    }

    /**
     * One spelling per normalized name (the first one given wins, as in TagNames.parse), ordered by normalized name.
     */
    static List<String> inLockOrder(List<String> tagNames) {
        Map<String, String> byKey = new TreeMap<>();
        for (String name : tagNames) {
            byKey.putIfAbsent(normalizedKey(name), name);
        }
        return new ArrayList<>(byKey.values());
    }

    private static String normalizedKey(String name) {
        return name.strip().toLowerCase(Locale.ROOT);
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
