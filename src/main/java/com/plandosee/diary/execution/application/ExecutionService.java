package com.plandosee.diary.execution.application;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plandosee.diary.common.concurrency.WriteTransactions;
import com.plandosee.diary.common.config.CurrentUserProvider;
import com.plandosee.diary.common.id.IdGenerator;
import com.plandosee.diary.common.time.SeoulDates;
import com.plandosee.diary.execution.application.port.ExecutionLogMapper;
import com.plandosee.diary.execution.domain.ActualMinutes;
import com.plandosee.diary.execution.domain.ExecutionLogRow;
import com.plandosee.diary.todo.application.TodoService;

/**
 * Execution records are stored separately from the plan. Recording one never changes plan or todo estimates.
 */
@Service
public class ExecutionService {

    private final ExecutionLogMapper executionLogMapper;
    private final TodoService todoService;
    private final CurrentUserProvider currentUserProvider;
    private final IdGenerator idGenerator;
    private final SeoulDates seoulDates;
    private final WriteTransactions writes;

    public ExecutionService(ExecutionLogMapper executionLogMapper, TodoService todoService,
                            CurrentUserProvider currentUserProvider, IdGenerator idGenerator, SeoulDates seoulDates,
                            WriteTransactions writes) {
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
     * {@link com.plandosee.diary.todo.application.TodoDeletedException} with nothing stored.
     */
    public UUID record(UUID todoId, OffsetDateTime startedAt, OffsetDateTime endedAt, String blockerReason) {
        int actualMinutes = ActualMinutes.between(startedAt, endedAt);
        return writes.run(() -> {
            todoService.lockOwned(todoId);
            ExecutionLogRow log = new ExecutionLogRow();
            log.setId(idGenerator.newId());
            log.setTodoId(todoId);
            log.setStartedAt(startedAt);
            log.setEndedAt(endedAt);
            log.setActualMinutes(actualMinutes);
            log.setBlockerReason(normalizeBlocker(blockerReason));
            log.setCreatedAt(seoulDates.now().atOffset(ZoneOffset.UTC));
            executionLogMapper.insert(log);
            return log.getId();
        });
    }

    @Transactional(readOnly = true)
    public List<ExecutionLogRow> listForTodo(UUID todoId) {
        todoService.requireOwned(todoId);
        return executionLogMapper.listForTodoOwned(currentUserProvider.currentUserId(), todoId);
    }

    /** The todo's records with their total, read together (S03). */
    @Transactional(readOnly = true)
    public TodoExecutionLogs logsForTodo(UUID todoId) {
        return TodoExecutionLogs.of(listForTodo(todoId));
    }

    static String normalizeBlocker(String blockerReason) {
        if (blockerReason == null) {
            return null;
        }
        String trimmed = blockerReason.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
