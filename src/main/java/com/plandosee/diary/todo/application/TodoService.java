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
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.plandosee.diary.common.concurrency.WriteTransactions;
import com.plandosee.diary.common.config.CurrentUserProvider;
import com.plandosee.diary.common.domain.EditOutcome;
import com.plandosee.diary.common.domain.EditSnapshot;
import com.plandosee.diary.common.domain.Priority;
import com.plandosee.diary.common.error.NotFoundException;
import com.plandosee.diary.common.id.IdGenerator;
import com.plandosee.diary.common.paging.Page;
import com.plandosee.diary.common.paging.PageInfo;
import com.plandosee.diary.common.paging.PageSettings;
import com.plandosee.diary.common.time.SeoulDates;
import com.plandosee.diary.plan.application.PlanService;
import com.plandosee.diary.todo.application.port.TagMapper;
import com.plandosee.diary.todo.application.port.TodoMapper;
import com.plandosee.diary.todo.domain.CompletionEventRow;
import com.plandosee.diary.todo.domain.DueFilter;
import com.plandosee.diary.todo.domain.TagRow;
import com.plandosee.diary.todo.domain.TodoFilter;
import com.plandosee.diary.todo.domain.TodoRevisionRow;
import com.plandosee.diary.todo.domain.TodoRow;
import com.plandosee.diary.todo.domain.TodoRules;
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
    private final PageSettings pageSettings;

    public TodoService(TodoMapper todoMapper, TagMapper tagMapper, PlanService planService,
                       CurrentUserProvider currentUserProvider, IdGenerator idGenerator, SeoulDates seoulDates,
                       WriteTransactions writes, PageSettings pageSettings) {
        this.pageSettings = pageSettings;
        this.todoMapper = todoMapper;
        this.tagMapper = tagMapper;
        this.planService = planService;
        this.currentUserProvider = currentUserProvider;
        this.idGenerator = idGenerator;
        this.seoulDates = seoulDates;
        this.writes = writes;
    }

    public UUID create(UUID planId, TodoCommand command) {
        requireCommand(command);
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
     * Only title, due date, priority, estimate, and tags change. Status, plan, and id are untouched.
     * Under the todo row lock, the values just before the edit are stored as the next todo_revisions row in
     * the same transaction as the update. When the submitted content equals the stored content (tags
     * compared by normalized name), nothing is written and the result is UNCHANGED.
     */
    public EditOutcome update(UUID todoId, TodoCommand command) {
        return update(todoId, command, null);
    }

    /**
     * expectedVersion is the version the edit form was opened with (null: no check). Under the todo row lock:
     * unchanged content is UNCHANGED; a different version is {@link TodoStaleException} with the latest todo
     * (tags and status included) and the differing fields, and nothing is written (no revision). A todo deleted
     * before the save is {@link TodoDeletedException}; one that never existed or is not owned is 404.
     * Completion and reopen never change the version, so they never make an open edit form stale.
     */
    public EditOutcome update(UUID todoId, TodoCommand command, Integer expectedVersion) {
        requireCommand(command);
        return writes.run(() -> {
            UUID userId = currentUserProvider.currentUserId();
            TodoRow todo = todoMapper.lockActiveOwned(userId, todoId);
            if (todo == null) {
                throw deletedOrMissing(userId, todoId);
            }
            List<TagRow> currentTagRows = tagMapper.listForTodos(userId, List.of(todoId));
            List<String> currentTags = currentTagRows.stream().map(TagRow::getName).toList();
            List<String> newTags = command.tagNames();
            List<String> changed = changedFields(todo, currentTags, command, newTags);
            if (changed.isEmpty()) {
                return EditOutcome.UNCHANGED;
            }
            if (expectedVersion != null && expectedVersion != todo.getVersion()) {
                todo.setTags(new ArrayList<>(currentTagRows));
                throw new TodoStaleException(todo, changed);
            }
            OffsetDateTime now = now();
            todoMapper.insertRevision(revisionOf(todo, currentTags, now));
            // updateContentOwned guards on the version read under the lock and moves it on by one.
            applyContent(todo, command);
            todo.setUpdatedAt(now);
            if (todoMapper.updateContentOwned(userId, todo) != 1) {
                throw new NotFoundException("todo");
            }
            replaceTags(userId, todoId, newTags, now);
            return EditOutcome.UPDATED;
        });
    }

    private TodoRevisionRow revisionOf(TodoRow todo, List<String> tagNames, OffsetDateTime now) {
        TodoRevisionRow revision = new TodoRevisionRow();
        revision.setId(idGenerator.newId());
        revision.setTodoId(todo.getId());
        revision.setRevisionNo(todoMapper.nextRevisionNo(todo.getId()));
        revision.setTitle(todo.getTitle());
        revision.setDueDate(todo.getDueDate());
        revision.setPriority(todo.getPriority());
        revision.setEstimatedMinutes(todo.getEstimatedMinutes());
        revision.setTagNames(tagNames);
        revision.setRevisedAt(now);
        return revision;
    }

    /**
     * TodoForm field names whose submitted value differs from the stored todo, in form order; tags
     * compared as sets of normalized names. Empty means the same content.
     */
    static List<String> changedFields(TodoRow todo, List<String> currentTags, TodoCommand command, List<String> newTags) {
        List<String> changed = new ArrayList<>();
        if (!todo.getTitle().equals(command.title())) {
            changed.add("title");
        }
        if (!java.util.Objects.equals(todo.getDueDate(), command.dueDate())) {
            changed.add("dueDate");
        }
        if (todo.getPriority() != command.priority()) {
            changed.add("priority");
        }
        if (todo.getEstimatedMinutes() != command.estimatedMinutes()) {
            changed.add("estimatedMinutes");
        }
        if (!normalizedSet(currentTags).equals(normalizedSet(newTags))) {
            changed.add("tags");
        }
        return changed;
    }

    static java.util.Set<String> normalizedSet(List<String> names) {
        java.util.Set<String> set = new java.util.TreeSet<>();
        for (String name : names) {
            set.add(TodoRules.tagKey(name));
        }
        return set;
    }

    /**
     * Soft delete. Deleting a todo that is already deleted (another tab) is {@link TodoDeletedException},
     * so the user is sent to the list with the "already deleted" notice instead of a 404; a todo that never existed
     * or is not owned stays 404.
     */
    public UUID delete(UUID todoId) {
        return writes.run(() -> {
            UUID userId = currentUserProvider.currentUserId();
            TodoRow todo = todoMapper.lockActiveOwned(userId, todoId);
            if (todo == null) {
                throw deletedOrMissing(userId, todoId);
            }
            todoMapper.softDeleteOwned(userId, todoId, now());
            return todo.getPlanId();
        });
    }

    /**
     * Locks the owned active todo in the caller's transaction: completion and execution records take this
     * lock so they serialize with each other and with a concurrent delete.
     * <ul>
     *   <li>Not active when the request started (already deleted, never existed, not owned): NotFoundException
     *       (404).</li>
     *   <li>Active when the request started but deleted by a concurrent request before the lock was granted:
     *       {@link TodoDeletedException}, so the caller can send the user to the list.</li>
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

    /**
     * One page of the filtered todo list. Count and rows use the same conditions and are read in one
     * repeatable-read snapshot; a page past the end shows the last page. Tags are attached to the page rows only.
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page<TodoRow> searchPage(UUID planId, String query, TodoStatus status, Priority priority,
                                    UUID tagId, DueFilter due, TodoSort sort, int requestedPage) {
        UUID userId = currentUserProvider.currentUserId();
        planService.requireOwned(planId);
        TodoFilter filter = new TodoFilter(userId, planId, query, status, priority, tagId, due, sort, seoulDates.today());
        PageInfo info = pageSettings.page(requestedPage, todoMapper.countSearch(filter));
        List<TodoRow> todos = info.totalCount() == 0 ? List.of()
                : todoMapper.search(filter.page(info.limit(), info.offset()));
        attachTags(userId, todos);
        return new Page<>(todos, info);
    }

    @Transactional(readOnly = true)
    public List<CompletionEventRow> completionEvents(UUID todoId) {
        UUID userId = currentUserProvider.currentUserId();
        if (todoMapper.findActiveOwned(userId, todoId, seoulDates.today()) == null) {
            throw new NotFoundException("todo");
        }
        return todoMapper.listCompletionEventsOwned(userId, todoId);
    }

    /**
     * The latest stored todo (tags and status included) and the TodoForm fields where the given input differs
     * from it, for an edit form shown again after a failed save.
     */
    @Transactional(readOnly = true)
    public EditSnapshot<TodoRow> latestForEdit(UUID todoId, TodoCommand input) {
        TodoRow latest = get(todoId);
        List<String> tags = latest.getTags().stream().map(TagRow::getName).toList();
        return new EditSnapshot<>(latest, changedFields(latest, tags, input, input.tagNames()));
    }

    @Transactional(readOnly = true)
    public List<TagRow> tags() {
        return tagMapper.listActiveOwned(currentUserProvider.currentUserId());
    }

    /** Owned but soft-deleted: TodoDeletedException(planId); never existed or not owned: NotFoundException. */
    private NotFoundException deletedOrMissing(UUID userId, UUID todoId) {
        return deletedMeanwhile(userId, todoId);
    }

    private NotFoundException deletedMeanwhile(UUID userId, UUID todoId) {
        UUID planId = todoMapper.findPlanIdOwnedIncludingDeleted(userId, todoId);
        return planId == null ? new NotFoundException("todo") : new TodoDeletedException(planId);
    }

    /**
     * A TodoCommand checks itself when it is built (the same rules and codes as TodoForm), so only a
     * missing command is left to reject here.
     */
    private static void requireCommand(TodoCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("todo command");
        }
    }

    private void applyContent(TodoRow todo, TodoCommand command) {
        todo.setTitle(command.title());
        todo.setDueDate(command.dueDate());
        todo.setPriority(command.priority());
        todo.setEstimatedMinutes(command.estimatedMinutes());
    }

    /**
     * New tag rows hold the unique-index entry of their normalized name until commit. Inserting in the input order
     * let two saves with the same new tags in opposite order deadlock; every transaction now inserts
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
            byKey.putIfAbsent(TodoRules.tagKey(name), name);
        }
        return new ArrayList<>(byKey.values());
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

    /**
     * Account deletion only (ADR-35): physically deletes every todo with its completion and reopen events, revisions and tag links, and every tag of the logged-in person, soft-deleted rows included.
     * Joins the caller's transaction; the caller (AccountService) deletes in foreign-key order: execution, todo,
     * review, plan, then the person.
     */
    @Transactional
    public void deleteAllOfCurrentUser() {
        UUID userId = currentUserProvider.currentUserId();
        todoMapper.deleteReopenEventsOwnedBy(userId);
        todoMapper.deleteCompletionEventsOwnedBy(userId);
        todoMapper.deleteRevisionsOwnedBy(userId);
        todoMapper.deleteTodoTagsOwnedBy(userId);
        todoMapper.deleteAllOwnedBy(userId);
        tagMapper.deleteAllOwnedBy(userId);
    }
}
