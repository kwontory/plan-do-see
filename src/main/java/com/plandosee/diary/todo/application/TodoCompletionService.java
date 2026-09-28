package com.plandosee.diary.todo.application;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.plandosee.diary.common.config.CurrentUserProvider;
import com.plandosee.diary.common.error.DomainRuleException;
import com.plandosee.diary.common.error.NotFoundException;
import com.plandosee.diary.common.id.IdGenerator;
import com.plandosee.diary.common.time.SeoulDates;
import com.plandosee.diary.todo.domain.CompletionEventRow;
import com.plandosee.diary.todo.domain.TodoRow;
import com.plandosee.diary.todo.domain.TodoStatus;
import com.plandosee.diary.todo.infrastructure.TodoMapper;

/**
 * DEC-05 / ADR-05: completion is idempotent on the server. The todo row lock serializes concurrent requests,
 * and the unique constraints are the last line of defence.
 */
@Service
public class TodoCompletionService {

    private final TodoMapper todoMapper;
    private final CurrentUserProvider currentUserProvider;
    private final IdGenerator idGenerator;
    private final SeoulDates seoulDates;
    private final TransactionTemplate transactionTemplate;

    public TodoCompletionService(TodoMapper todoMapper, CurrentUserProvider currentUserProvider,
                                 IdGenerator idGenerator, SeoulDates seoulDates,
                                 TransactionTemplate transactionTemplate) {
        this.todoMapper = todoMapper;
        this.currentUserProvider = currentUserProvider;
        this.idGenerator = idGenerator;
        this.seoulDates = seoulDates;
        this.transactionTemplate = transactionTemplate;
    }

    public TransitionResult complete(UUID todoId, UUID idempotencyKey) {
        if (idempotencyKey == null) {
            throw new DomainRuleException("idempotencyKey", "완료 요청 식별값이 없습니다. 화면을 새로고침한 뒤 다시 시도하세요.");
        }
        try {
            return transactionTemplate.execute(status -> completeInTransaction(todoId, idempotencyKey));
        } catch (DuplicateKeyException duplicate) {
            // Reached only after the owned-todo lock succeeded: a concurrent request committed first.
            // Report the current state instead of a DB error.
            Boolean sameKey = transactionTemplate.execute(status -> todoMapper.countEventsByKeyOwned(currentUserProvider.currentUserId(), todoId, idempotencyKey) > 0);
            return Boolean.TRUE.equals(sameKey) ? TransitionResult.REPLAYED : TransitionResult.ALREADY_COMPLETED;
        }
    }

    public TransitionResult reopen(UUID todoId) {
        return transactionTemplate.execute(status -> {
            TodoRow todo = lock(todoId);
            if (todo.getStatus() == TodoStatus.IN_PROGRESS) {
                return TransitionResult.ALREADY_IN_PROGRESS;
            }
            todoMapper.markInProgress(todoId, now());
            return TransitionResult.REOPENED;
        });
    }

    private TransitionResult completeInTransaction(UUID todoId, UUID idempotencyKey) {
        TodoRow todo = lock(todoId);
        if (todoMapper.countEventsByKeyOwned(currentUserProvider.currentUserId(), todoId, idempotencyKey) > 0) {
            return TransitionResult.REPLAYED;
        }
        if (todo.getStatus() == TodoStatus.COMPLETED) {
            return TransitionResult.ALREADY_COMPLETED;
        }
        OffsetDateTime now = now();
        int cycleNo = todo.getCompletionCycle() + 1;
        if (todoMapper.markCompleted(todoId, cycleNo, now) != 1) {
            return TransitionResult.ALREADY_COMPLETED;
        }
        CompletionEventRow event = new CompletionEventRow();
        event.setId(idGenerator.newId());
        event.setTodoId(todoId);
        event.setIdempotencyKey(idempotencyKey);
        event.setCycleNo(cycleNo);
        event.setCompletedAt(now);
        event.setCreatedAt(now);
        todoMapper.insertCompletionEvent(event);
        return TransitionResult.COMPLETED;
    }

    private TodoRow lock(UUID todoId) {
        TodoRow todo = todoMapper.lockActiveOwned(currentUserProvider.currentUserId(), todoId);
        if (todo == null) {
            throw new NotFoundException("todo");
        }
        return todo;
    }

    private OffsetDateTime now() {
        return seoulDates.now().atOffset(ZoneOffset.UTC);
    }
}
