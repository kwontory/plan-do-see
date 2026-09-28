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
import com.plandosee.diary.todo.domain.CompletionHistoryCounts;
import com.plandosee.diary.todo.domain.CompletionHistoryEntry;
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
        validate(command);
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
            replaceTags(userId, todo.getId(), tagNames(command), now);
            return todo.getId();
        });
    }

    /**
     * Only title, due date, priority, estimate, and tags change (ADR-07). Status, plan, and id are untouched.
     * ADR-16: under the todo row lock, the values just before the edit are stored as the next todo_revisions row in
     * the same transaction as the update. ADR-18 E5: when the submitted content equals the stored content (tags
     * compared by normalized name), nothing is written and the result is UNCHANGED.
     */
    public EditOutcome update(UUID todoId, TodoCommand command) {
        validate(command);
        return writes.run(() -> {
            UUID userId = currentUserProvider.currentUserId();
            TodoRow todo = todoMapper.lockActiveOwned(userId, todoId);
            if (todo == null) {
                throw new NotFoundException("todo");
            }
            List<String> currentTags = tagNamesOf(userId, todoId);
            List<String> newTags = tagNames(command);
            if (sameContent(todo, currentTags, command, newTags)) {
                return EditOutcome.UNCHANGED;
            }
            OffsetDateTime now = now();
            todoMapper.insertRevision(revisionOf(todo, currentTags, now));
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

    /** Display names of the todo's active tags, ordered by normalized name (the stored snapshot order). */
    private List<String> tagNamesOf(UUID userId, UUID todoId) {
        return tagMapper.listForTodos(userId, List.of(todoId)).stream().map(TagRow::getName).toList();
    }

    /** ADR-18 E5: the form content is the same as stored; tags compared as sets of normalized names. */
    static boolean sameContent(TodoRow todo, List<String> currentTags, TodoCommand command, List<String> newTags) {
        return todo.getTitle().equals(command.title().strip())
                && java.util.Objects.equals(todo.getDueDate(), command.dueDate())
                && todo.getPriority() == command.priority()
                && todo.getEstimatedMinutes() == command.estimatedMinutes()
                && normalizedSet(currentTags).equals(normalizedSet(newTags));
    }

    static java.util.Set<String> normalizedSet(List<String> names) {
        java.util.Set<String> set = new java.util.TreeSet<>();
        for (String name : names) {
            set.add(normalizedKey(name));
        }
        return set;
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

    /**
     * ADR-21: one page of the filtered list (S02). Count and rows use the same conditions and are read in one
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

    /** ADR-16: the owned active todo's edit history, newest first. NotFoundException otherwise. */
    @Transactional(readOnly = true)
    public List<TodoRevisionRow> revisions(UUID todoId) {
        UUID userId = currentUserProvider.currentUserId();
        if (todoMapper.findActiveOwned(userId, todoId, seoulDates.today()) == null) {
            throw new NotFoundException("todo");
        }
        return todoMapper.listRevisionsOwned(userId, todoId);
    }

    /**
     * ADR-16 / ADR-21: one page of the merged completion and reopen history in time order, with the counts of all
     * events, in one repeatable-read snapshot. NotFoundException when the todo is not an owned active todo.
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public CompletionHistory completionHistory(UUID todoId, int requestedPage) {
        UUID userId = currentUserProvider.currentUserId();
        if (todoMapper.findActiveOwned(userId, todoId, seoulDates.today()) == null) {
            throw new NotFoundException("todo");
        }
        CompletionHistoryCounts counts = todoMapper.countCompletionHistoryOwned(userId, todoId);
        PageInfo info = pageSettings.page(requestedPage, counts.total());
        List<CompletionHistoryEntry> entries = info.totalCount() == 0 ? List.of()
                : todoMapper.listCompletionHistoryOwned(userId, todoId, info.limit(), info.offset());
        return new CompletionHistory(entries, counts.getCompletionCount(), counts.getReopenCount(), info);
    }

    @Transactional(readOnly = true)
    public List<TagRow> tags() {
        return tagMapper.listActiveOwned(currentUserProvider.currentUserId());
    }

    private NotFoundException deletedMeanwhile(UUID userId, UUID todoId) {
        UUID planId = todoMapper.findPlanIdOwnedIncludingDeleted(userId, todoId);
        return planId == null ? new NotFoundException("todo") : new TodoDeletedException(planId);
    }

    /**
     * ADR-22: the same rules and codes as TodoForm (TodoRules), checked at the entrance whatever the caller.
     */
    private static void validate(TodoCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("todo command");
        }
        TodoRules.check(command.title(), command.priority(), command.estimatedMinutes(), command.tagNames());
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

    /** Parsed tag names without blanks (a direct call may pass null or blank entries). */
    private static List<String> tagNames(TodoCommand command) {
        if (command.tagNames() == null) {
            return List.of();
        }
        return command.tagNames().stream().filter(name -> name != null && !name.isBlank()).map(String::strip).toList();
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
