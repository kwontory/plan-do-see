package com.plandosee.diary.execution.application.port;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.plandosee.diary.execution.domain.ExecutionLogRevisionRow;
import com.plandosee.diary.execution.domain.ExecutionLogRow;
import com.plandosee.diary.execution.domain.ExecutionLogTotals;

@Mapper
public interface ExecutionLogMapper {

    int insert(ExecutionLogRow log);

    List<ExecutionLogRow> listForTodoOwned(@Param("userId") UUID userId, @Param("todoId") UUID todoId);

    List<ExecutionLogRow> listForTodoOwnedPage(@Param("userId") UUID userId, @Param("todoId") UUID todoId,
                                               @Param("limit") int limit, @Param("offset") long offset);

    ExecutionLogTotals totalsForTodoOwned(@Param("userId") UUID userId, @Param("todoId") UUID todoId);

    /** Physically deletes every execution log of the owner's todos (account deletion, ADR-35: every row, soft-deleted or not). */
    int deleteAllOwnedBy(@Param("userId") UUID userId);

    /**
     * One of the owner's records on an active todo of an active plan whose half-open period overlaps
     * {@code [startedAt, endedAt)} (both having a length), except {@code excludeId}; the earliest one, null when none.
     * Called under the per-person lock.
     */
    ExecutionLogRow findOverlapOwned(@Param("userId") UUID userId, @Param("startedAt") OffsetDateTime startedAt,
                                     @Param("endedAt") OffsetDateTime endedAt, @Param("excludeId") UUID excludeId);

    /** The owned record on an active todo of an active plan, with its todo title; null when none. */
    ExecutionLogRow findActiveOwned(@Param("userId") UUID userId, @Param("logId") UUID logId);

    /** Same as findActiveOwned, locking the record row without waiting (FOR UPDATE NOWAIT). */
    ExecutionLogRow lockActiveOwned(@Param("userId") UUID userId, @Param("logId") UUID logId);

    int nextRevisionNo(@Param("logId") UUID logId);

    int insertRevision(ExecutionLogRevisionRow revision);

    /** Writes start, end, actual minutes and blocker reason of the locked owned record and adds 1 to its version. */
    int updateOwned(@Param("userId") UUID userId, @Param("log") ExecutionLogRow log);

    /** Physically deletes the revisions of the owner's records (account deletion, before the records). */
    int deleteRevisionsOwnedBy(@Param("userId") UUID userId);
}
