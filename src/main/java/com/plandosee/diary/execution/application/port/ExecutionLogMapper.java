package com.plandosee.diary.execution.application.port;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}
