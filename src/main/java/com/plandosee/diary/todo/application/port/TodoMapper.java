package com.plandosee.diary.todo.application.port;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.plandosee.diary.todo.domain.CompletionEventRow;
import com.plandosee.diary.todo.domain.CompletionHistoryCounts;
import com.plandosee.diary.todo.domain.CompletionHistoryEntry;
import com.plandosee.diary.todo.domain.ReopenEventRow;
import com.plandosee.diary.todo.domain.TodoFilter;
import com.plandosee.diary.todo.domain.TodoRevisionRow;
import com.plandosee.diary.todo.domain.TodoRow;

@Mapper
public interface TodoMapper {

    int insert(TodoRow todo);

    /** Includes the display flags overdue / dueToday judged against today (Seoul). */
    TodoRow findActiveOwned(@Param("userId") UUID userId, @Param("todoId") UUID todoId, @Param("today") LocalDate today);

    /** Plan id of an owned todo in an active plan, deleted or not; null when it never existed or is not owned. */
    UUID findPlanIdOwnedIncludingDeleted(@Param("userId") UUID userId, @Param("todoId") UUID todoId);

    /** Row lock for writes; the display flags are not computed (always false). */
    TodoRow lockActiveOwned(@Param("userId") UUID userId, @Param("todoId") UUID todoId);

    /** The whole filtered list, or one page of it when the filter carries a limit. */
    List<TodoRow> search(TodoFilter filter);

    /** Rows of the filtered list, with the same conditions as search. */
    long countSearch(TodoFilter filter);

    int updateContentOwned(@Param("userId") UUID userId, @Param("todo") TodoRow todo);

    int softDeleteOwned(@Param("userId") UUID userId, @Param("todoId") UUID todoId, @Param("now") OffsetDateTime now);

    int markCompleted(@Param("todoId") UUID todoId, @Param("cycleNo") int cycleNo, @Param("completedAt") OffsetDateTime completedAt);

    int markInProgress(@Param("todoId") UUID todoId, @Param("now") OffsetDateTime now);

    int countEventsByKeyOwned(@Param("userId") UUID userId, @Param("todoId") UUID todoId,
                              @Param("idempotencyKey") UUID idempotencyKey);

    int insertCompletionEvent(CompletionEventRow event);

    List<CompletionEventRow> listCompletionEventsOwned(@Param("userId") UUID userId, @Param("todoId") UUID todoId);

    int nextRevisionNo(@Param("todoId") UUID todoId);

    int insertRevision(TodoRevisionRow revision);

    List<TodoRevisionRow> listRevisionsOwned(@Param("userId") UUID userId, @Param("todoId") UUID todoId);

    int insertReopenEvent(ReopenEventRow event);

    List<CompletionHistoryEntry> listCompletionHistoryOwned(@Param("userId") UUID userId, @Param("todoId") UUID todoId,
                                                            @Param("limit") int limit, @Param("offset") long offset);

    CompletionHistoryCounts countCompletionHistoryOwned(@Param("userId") UUID userId, @Param("todoId") UUID todoId);

    /** Physically deletes the owner's reopen events (account deletion, ADR-35: every row, soft-deleted or not). */
    int deleteReopenEventsOwnedBy(@Param("userId") UUID userId);

    /** Physically deletes the owner's completion events (after their reopen events). */
    int deleteCompletionEventsOwnedBy(@Param("userId") UUID userId);

    /** Physically deletes the owner's todo revisions. */
    int deleteRevisionsOwnedBy(@Param("userId") UUID userId);

    /** Physically deletes the tag links of the owner's todos. */
    int deleteTodoTagsOwnedBy(@Param("userId") UUID userId);

    /** Physically deletes the owner's todos (after every child row). */
    int deleteAllOwnedBy(@Param("userId") UUID userId);
}
