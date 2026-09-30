package com.plandosee.diary.execution.application;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.plandosee.diary.common.concurrency.WriteTransactions;
import com.plandosee.diary.common.config.CurrentUserProvider;
import com.plandosee.diary.common.domain.EditOutcome;
import com.plandosee.diary.common.domain.EditSnapshot;
import com.plandosee.diary.common.error.NotFoundException;
import com.plandosee.diary.common.id.IdGenerator;
import com.plandosee.diary.common.paging.PageInfo;
import com.plandosee.diary.common.paging.PageSettings;
import com.plandosee.diary.common.time.SeoulDates;
import com.plandosee.diary.execution.application.port.ExecutionLogMapper;
import com.plandosee.diary.execution.domain.ExecutionLogRevisionRow;
import com.plandosee.diary.execution.domain.ExecutionLogRow;
import com.plandosee.diary.execution.domain.ExecutionLogTotals;
import com.plandosee.diary.execution.domain.ExecutionOverlap;
import com.plandosee.diary.todo.application.TodoService;
import com.plandosee.diary.user.application.UserService;

/**
 * Execution records are stored separately from the plan. Recording one never changes plan or todo estimates.
 * Records of one person never overlap (half-open periods, ADR-40): adding and editing check the person's other
 * records under a per-person lock (UserService.lockForOwnedWrites, no waiting), taken before any other row lock.
 * An edit keeps the previous values in execution_log_revisions (not shown on screen, exported).
 */
@Service
public class ExecutionService {

    private final ExecutionLogMapper executionLogMapper;
    private final TodoService todoService;
    private final CurrentUserProvider currentUserProvider;
    private final IdGenerator idGenerator;
    private final SeoulDates seoulDates;
    private final WriteTransactions writes;
    private final PageSettings pageSettings;
    private final UserService userService;

    public ExecutionService(ExecutionLogMapper executionLogMapper, TodoService todoService,
                            CurrentUserProvider currentUserProvider, IdGenerator idGenerator, SeoulDates seoulDates,
                            WriteTransactions writes, PageSettings pageSettings, UserService userService) {
        this.pageSettings = pageSettings;
        this.userService = userService;
        this.executionLogMapper = executionLogMapper;
        this.todoService = todoService;
        this.currentUserProvider = currentUserProvider;
        this.idGenerator = idGenerator;
        this.seoulDates = seoulDates;
        this.writes = writes;
    }

    /**
     * Takes the owned todo's row lock (TodoService.lockOwned) so a concurrent delete cannot leave a log under a
     * just-deleted todo: either the log commits first and the delete follows, or the delete wins and this throws
     * {@link com.plandosee.diary.todo.application.TodoDeletedException} with nothing stored. The input is checked by
     * building the command first (the form's codes): Seoul-date range, period and its length limit,
     * blocker reason. A period overlapping another record of the person is
     * DomainRuleException("startedAt", {@link ExecutionOverlap#OVERLAPS}, todo title, start, end) and nothing is
     * stored.
     */
    public UUID record(UUID todoId, OffsetDateTime startedAt, OffsetDateTime endedAt, String blockerReason) {
        return record(todoId, new ExecutionCommand(startedAt, endedAt, blockerReason));
    }

    public UUID record(UUID todoId, ExecutionCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("execution command");
        }
        int actualMinutes = command.actualMinutes();
        return writes.run(() -> {
            UUID userId = currentUserProvider.currentUserId();
            userService.lockForOwnedWrites(userId);
            todoService.lockOwned(todoId);
            rejectOverlap(userId, command, null);
            ExecutionLogRow log = new ExecutionLogRow();
            log.setId(idGenerator.newId());
            log.setTodoId(todoId);
            log.setStartedAt(command.startedAt());
            log.setEndedAt(command.endedAt());
            log.setActualMinutes(actualMinutes);
            log.setBlockerReason(command.blockerReason());
            log.setCreatedAt(seoulDates.now().atOffset(ZoneOffset.UTC));
            executionLogMapper.insert(log);
            return log.getId();
        });
    }

    /** The owned record on an active todo (edit form), or NotFoundException. */
    @Transactional(readOnly = true)
    public ExecutionLogRow get(UUID logId) {
        ExecutionLogRow log = executionLogMapper.findActiveOwned(currentUserProvider.currentUserId(), logId);
        if (log == null) {
            throw new NotFoundException("execution");
        }
        return log;
    }

    /** The latest stored record and the form fields where the given input differs, for a form shown again. */
    @Transactional(readOnly = true)
    public EditSnapshot<ExecutionLogRow> latestForEdit(UUID logId, ExecutionCommand input) {
        ExecutionLogRow latest = get(logId);
        return new EditSnapshot<>(latest, changedFields(latest, input));
    }

    /**
     * Edits start, end and blocker reason of an owned record (ADR-40); the actual minutes are computed again.
     * expectedVersion is the version the form was opened with (null: no check). Under the per-person lock and then
     * the record row lock, in this order:
     * <ol>
     *   <li>same values as stored: nothing written, UNCHANGED</li>
     *   <li>version differs: {@link ExecutionStaleException} with the latest record and the differing fields</li>
     *   <li>the new period overlaps another record of the person (itself excluded): DomainRuleException
     *       ({@link ExecutionOverlap#OVERLAPS})</li>
     *   <li>otherwise the previous values go to execution_log_revisions and the record is updated (version + 1),
     *       together</li>
     * </ol>
     * NotFoundException for a record that is not the person's or whose todo or plan is deleted.
     */
    public ExecutionEditResult revise(UUID logId, ExecutionCommand command, Integer expectedVersion) {
        Objects.requireNonNull(command, "execution command");
        return writes.run(() -> {
            UUID userId = currentUserProvider.currentUserId();
            userService.lockForOwnedWrites(userId);
            ExecutionLogRow current = executionLogMapper.lockActiveOwned(userId, logId);
            if (current == null) {
                throw new NotFoundException("execution");
            }
            List<String> changed = changedFields(current, command);
            if (changed.isEmpty()) {
                return new ExecutionEditResult(EditOutcome.UNCHANGED, current.getTodoId());
            }
            if (expectedVersion != null && expectedVersion != current.getVersion()) {
                throw new ExecutionStaleException(current, changed);
            }
            rejectOverlap(userId, command, logId);
            OffsetDateTime now = seoulDates.now().atOffset(ZoneOffset.UTC);
            ExecutionLogRevisionRow revision = new ExecutionLogRevisionRow();
            revision.setId(idGenerator.newId());
            revision.setExecutionLogId(logId);
            revision.setRevisionNo(executionLogMapper.nextRevisionNo(logId));
            revision.setStartedAt(current.getStartedAt());
            revision.setEndedAt(current.getEndedAt());
            revision.setActualMinutes(current.getActualMinutes());
            revision.setBlockerReason(current.getBlockerReason());
            revision.setRevisedAt(now);
            executionLogMapper.insertRevision(revision);
            current.setStartedAt(command.startedAt());
            current.setEndedAt(command.endedAt());
            current.setActualMinutes(command.actualMinutes());
            current.setBlockerReason(command.blockerReason());
            if (executionLogMapper.updateOwned(userId, current) != 1) {
                throw new NotFoundException("execution");
            }
            return new ExecutionEditResult(EditOutcome.UPDATED, current.getTodoId());
        });
    }

    private void rejectOverlap(UUID userId, ExecutionCommand command, UUID excludeId) {
        if (!ExecutionOverlap.hasLength(command.startedAt(), command.endedAt())) {
            return;
        }
        ExecutionLogRow other = executionLogMapper.findOverlapOwned(userId, command.startedAt(), command.endedAt(),
                excludeId);
        if (other != null) {
            throw ExecutionOverlap.rejected(other);
        }
    }

    /** ExecutionForm field names whose value differs from the stored record (same instant, same text). */
    static List<String> changedFields(ExecutionLogRow log, ExecutionCommand command) {
        List<String> changed = new ArrayList<>();
        if (!log.getStartedAt().isEqual(command.startedAt())) {
            changed.add("startedAt");
        }
        if (!log.getEndedAt().isEqual(command.endedAt())) {
            changed.add("endedAt");
        }
        if (!Objects.equals(log.getBlockerReason(), command.blockerReason())) {
            changed.add("blockerReason");
        }
        return changed;
    }

    @Transactional(readOnly = true)
    public List<ExecutionLogRow> listForTodo(UUID todoId) {
        todoService.requireOwned(todoId);
        return executionLogMapper.listForTodoOwned(currentUserProvider.currentUserId(), todoId);
    }

    /** The todo's records with their total, read together (todo detail page). */
    @Transactional(readOnly = true)
    public TodoExecutionLogs logsForTodo(UUID todoId) {
        return TodoExecutionLogs.of(listForTodo(todoId));
    }

    /**
     * One page of the todo's records, with the count and minute total of all of them, in one
     * repeatable-read snapshot. A page past the end shows the last page.
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public TodoExecutionLogs logsForTodo(UUID todoId, int requestedPage) {
        todoService.requireOwned(todoId);
        UUID userId = currentUserProvider.currentUserId();
        ExecutionLogTotals totals = executionLogMapper.totalsForTodoOwned(userId, todoId);
        PageInfo info = pageSettings.page(requestedPage, totals.getCount());
        List<ExecutionLogRow> logs = info.totalCount() == 0 ? List.of()
                : executionLogMapper.listForTodoOwnedPage(userId, todoId, info.limit(), info.offset());
        return new TodoExecutionLogs(logs, totals.getActualMinutes(), info);
    }

    /**
     * Account deletion only (ADR-35): physically deletes every execution log of the logged-in person and their edit
     * revisions (ADR-40), soft-deleted rows included.
     * Joins the caller's transaction; the caller (AccountService) deletes in foreign-key order: execution, todo,
     * review, plan, then the person.
     */
    @Transactional
    public void deleteAllOfCurrentUser() {
        UUID userId = currentUserProvider.currentUserId();
        executionLogMapper.deleteRevisionsOwnedBy(userId);
        executionLogMapper.deleteAllOwnedBy(userId);
    }
}
