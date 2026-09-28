package com.plandosee.diary.todo.application;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.plandosee.diary.common.concurrency.WriteTransactions;
import com.plandosee.diary.common.config.CurrentUserProvider;
import com.plandosee.diary.common.error.DomainRuleException;
import com.plandosee.diary.common.id.IdGenerator;
import com.plandosee.diary.common.time.SeoulDates;
import com.plandosee.diary.todo.application.port.TodoMapper;
import com.plandosee.diary.todo.domain.CompletionEventRow;
import com.plandosee.diary.todo.domain.TodoRow;
import com.plandosee.diary.todo.domain.TodoStatus;

/**
 * DEC-05 / ADR-05: completion is idempotent on the server. The todo row lock (FOR UPDATE NOWAIT, ADR-19) lets one
 * request at a time change the todo; a request that finds the row held is told at once to press again. The
 * idempotency key and unique constraints guarantee a single event. Each request is one write transaction
 * (ADR-15; retries are off by default): a retried attempt starts over from the lock.
 */
@Service
public class TodoCompletionService {

    public static final String KEY_MISSING = "todo.completion.keyMissing";

    private final TodoMapper todoMapper;
    private final TodoService todoService;
    private final CurrentUserProvider currentUserProvider;
    private final IdGenerator idGenerator;
    private final SeoulDates seoulDates;
    private final WriteTransactions writes;

    public TodoCompletionService(TodoMapper todoMapper, TodoService todoService, CurrentUserProvider currentUserProvider,
                                 IdGenerator idGenerator, SeoulDates seoulDates, WriteTransactions writes) {
        this.todoMapper = todoMapper;
        this.todoService = todoService;
        this.currentUserProvider = currentUserProvider;
        this.idGenerator = idGenerator;
        this.seoulDates = seoulDates;
        this.writes = writes;
    }

    public TransitionResult complete(UUID todoId, UUID idempotencyKey) {
        return completeWithOutcome(todoId, idempotencyKey).result();
    }

    public TransitionResult reopen(UUID todoId) {
        return reopenWithOutcome(todoId).result();
    }

    /**
     * Completion with the status and plan observed in the same transaction (ADR-14 C-1).
     *
     * @throws DomainRuleException {@link #KEY_MISSING} when the key is absent
     * @throws TodoDeletedException when the todo was deleted before the lock was taken
     */
    public TransitionOutcome completeWithOutcome(UUID todoId, UUID idempotencyKey) {
        if (idempotencyKey == null) {
            throw new DomainRuleException("idempotencyKey", KEY_MISSING);
        }
        try {
            return writes.run(() -> completeLocked(todoId, idempotencyKey));
        } catch (DuplicateKeyException duplicate) {
            // Reached only after the owned-todo lock succeeded: a concurrent request committed first.
            // Report the current state instead of a DB error.
            return writes.run(() -> {
                TodoRow current = todoService.requireOwned(todoId);
                boolean sameKey = todoMapper.countEventsByKeyOwned(currentUserProvider.currentUserId(), todoId, idempotencyKey) > 0;
                return new TransitionOutcome(sameKey ? TransitionResult.REPLAYED : TransitionResult.ALREADY_COMPLETED,
                        current.getStatus(), current.getPlanId());
            });
        }
    }

    /**
     * Reopen with the status and plan observed in the same transaction (ADR-14 C-1).
     *
     * @throws TodoDeletedException when the todo was deleted before the lock was taken
     */
    public TransitionOutcome reopenWithOutcome(UUID todoId) {
        return writes.run(() -> {
            TodoRow todo = todoService.lockOwned(todoId);
            if (todo.getStatus() == TodoStatus.IN_PROGRESS) {
                return new TransitionOutcome(TransitionResult.ALREADY_IN_PROGRESS, TodoStatus.IN_PROGRESS, todo.getPlanId());
            }
            todoMapper.markInProgress(todoId, now());
            return new TransitionOutcome(TransitionResult.REOPENED, TodoStatus.IN_PROGRESS, todo.getPlanId());
        });
    }

    private TransitionOutcome completeLocked(UUID todoId, UUID idempotencyKey) {
        TodoRow todo = todoService.lockOwned(todoId);
        if (todoMapper.countEventsByKeyOwned(currentUserProvider.currentUserId(), todoId, idempotencyKey) > 0) {
            return new TransitionOutcome(TransitionResult.REPLAYED, todo.getStatus(), todo.getPlanId());
        }
        if (todo.getStatus() == TodoStatus.COMPLETED) {
            return new TransitionOutcome(TransitionResult.ALREADY_COMPLETED, TodoStatus.COMPLETED, todo.getPlanId());
        }
        OffsetDateTime now = now();
        int cycleNo = todo.getCompletionCycle() + 1;
        if (todoMapper.markCompleted(todoId, cycleNo, now) != 1) {
            return new TransitionOutcome(TransitionResult.ALREADY_COMPLETED, TodoStatus.COMPLETED, todo.getPlanId());
        }
        CompletionEventRow event = new CompletionEventRow();
        event.setId(idGenerator.newId());
        event.setTodoId(todoId);
        event.setIdempotencyKey(idempotencyKey);
        event.setCycleNo(cycleNo);
        event.setCompletedAt(now);
        event.setCreatedAt(now);
        todoMapper.insertCompletionEvent(event);
        return new TransitionOutcome(TransitionResult.COMPLETED, TodoStatus.COMPLETED, todo.getPlanId());
    }

    private OffsetDateTime now() {
        return seoulDates.now().atOffset(ZoneOffset.UTC);
    }
}
