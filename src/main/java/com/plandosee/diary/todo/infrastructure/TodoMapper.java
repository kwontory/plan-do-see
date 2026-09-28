package com.plandosee.diary.todo.infrastructure;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.plandosee.diary.todo.domain.CompletionEventRow;
import com.plandosee.diary.todo.domain.TodoFilter;
import com.plandosee.diary.todo.domain.TodoRow;

@Mapper
public interface TodoMapper {

    int insert(TodoRow todo);

    TodoRow findActiveOwned(@Param("userId") UUID userId, @Param("todoId") UUID todoId);

    TodoRow lockActiveOwned(@Param("userId") UUID userId, @Param("todoId") UUID todoId);

    List<TodoRow> search(TodoFilter filter);

    int updateContentOwned(@Param("userId") UUID userId, @Param("todo") TodoRow todo);

    int softDeleteOwned(@Param("userId") UUID userId, @Param("todoId") UUID todoId, @Param("now") OffsetDateTime now);

    int markCompleted(@Param("todoId") UUID todoId, @Param("cycleNo") int cycleNo, @Param("completedAt") OffsetDateTime completedAt);

    int markInProgress(@Param("todoId") UUID todoId, @Param("now") OffsetDateTime now);

    int countEventsByKey(@Param("todoId") UUID todoId, @Param("idempotencyKey") UUID idempotencyKey);

    int insertCompletionEvent(CompletionEventRow event);

    List<CompletionEventRow> listCompletionEventsOwned(@Param("userId") UUID userId, @Param("todoId") UUID todoId);
}
