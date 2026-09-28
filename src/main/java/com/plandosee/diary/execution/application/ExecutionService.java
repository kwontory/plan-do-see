package com.plandosee.diary.execution.application;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plandosee.diary.common.config.CurrentUserProvider;
import com.plandosee.diary.common.error.NotFoundException;
import com.plandosee.diary.common.id.IdGenerator;
import com.plandosee.diary.common.time.SeoulDates;
import com.plandosee.diary.execution.domain.ActualMinutes;
import com.plandosee.diary.execution.domain.ExecutionLogRow;
import com.plandosee.diary.execution.infrastructure.ExecutionLogMapper;
import com.plandosee.diary.todo.infrastructure.TodoMapper;

/**
 * Execution records are stored separately from the plan. Recording one never changes plan or todo estimates.
 */
@Service
public class ExecutionService {

    private final ExecutionLogMapper executionLogMapper;
    private final TodoMapper todoMapper;
    private final CurrentUserProvider currentUserProvider;
    private final IdGenerator idGenerator;
    private final SeoulDates seoulDates;

    public ExecutionService(ExecutionLogMapper executionLogMapper, TodoMapper todoMapper,
                            CurrentUserProvider currentUserProvider, IdGenerator idGenerator, SeoulDates seoulDates) {
        this.executionLogMapper = executionLogMapper;
        this.todoMapper = todoMapper;
        this.currentUserProvider = currentUserProvider;
        this.idGenerator = idGenerator;
        this.seoulDates = seoulDates;
    }

    @Transactional
    public UUID record(UUID todoId, OffsetDateTime startedAt, OffsetDateTime endedAt, String blockerReason) {
        int actualMinutes = ActualMinutes.between(startedAt, endedAt);
        // Locks the owned active todo so a concurrent delete cannot leave a log under a just-deleted todo.
        if (todoMapper.lockActiveOwned(currentUserProvider.currentUserId(), todoId) == null) {
            throw new NotFoundException("todo");
        }
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
    }

    @Transactional(readOnly = true)
    public List<ExecutionLogRow> listForTodo(UUID todoId) {
        UUID userId = currentUserProvider.currentUserId();
        if (todoMapper.findActiveOwned(userId, todoId) == null) {
            throw new NotFoundException("todo");
        }
        return executionLogMapper.listForTodoOwned(userId, todoId);
    }

    static String normalizeBlocker(String blockerReason) {
        if (blockerReason == null) {
            return null;
        }
        String trimmed = blockerReason.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
