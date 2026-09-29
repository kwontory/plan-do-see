package com.plandosee.diary.export.application.port;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.plandosee.diary.execution.domain.ExecutionLogRow;
import com.plandosee.diary.export.domain.ExportTagRow;
import com.plandosee.diary.export.domain.ExportTodoTagRow;
import com.plandosee.diary.plan.domain.PlanRevisionRow;
import com.plandosee.diary.plan.domain.PlanRow;
import com.plandosee.diary.review.domain.ReviewRow;
import com.plandosee.diary.todo.domain.CompletionEventRow;
import com.plandosee.diary.todo.domain.ReopenEventRow;
import com.plandosee.diary.todo.domain.TodoRevisionRow;
import com.plandosee.diary.todo.domain.TodoRow;
import com.plandosee.diary.user.domain.UserRow;

/**
 * Export reads. Every query is owned by the server-resolved user, excludes soft-deleted rows and the children
 * of soft-deleted parents, and is ordered by id (todoTags by todo_id, tag_id).
 */
@Mapper
public interface ExportMapper {

    UserRow owner(@Param("userId") UUID userId);

    List<PlanRow> plans(@Param("userId") UUID userId);

    List<PlanRevisionRow> planRevisions(@Param("userId") UUID userId);

    List<TodoRow> todos(@Param("userId") UUID userId);

    List<ExportTagRow> tags(@Param("userId") UUID userId);

    List<ExportTodoTagRow> todoTags(@Param("userId") UUID userId);

    List<ExecutionLogRow> executionLogs(@Param("userId") UUID userId);

    List<CompletionEventRow> completionEvents(@Param("userId") UUID userId);

    List<TodoRevisionRow> todoRevisions(@Param("userId") UUID userId);

    List<ReopenEventRow> reopenEvents(@Param("userId") UUID userId);

    List<ReviewRow> reviews(@Param("userId") UUID userId);
}
