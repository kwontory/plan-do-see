package com.plandosee.diary.export.application;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.plandosee.diary.common.config.CurrentUserProvider;
import com.plandosee.diary.common.db.StatementBudget;
import com.plandosee.diary.common.time.SeoulDates;
import com.plandosee.diary.common.time.TimeConfig;
import com.plandosee.diary.execution.domain.ExecutionLogRow;
import com.plandosee.diary.export.application.port.ExportMapper;
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
 * DEC-08 / T06-C35, T06-C36, T06-C58: one UTF-8 JSON document of every active owned record.
 * All reads share one read-only repeatable-read snapshot under the aggregate statement timeout (ADR-19). Field order follows exportContract.topLevelFields.
 * Dates are YYYY-MM-DD; timestamps are ISO-8601 with the Asia/Seoul offset (same instant as stored).
 * The owner carries id and nickname only; email and credential data are never read.
 */
@Service
public class ExportService {

    /** 2.1.0 (ADR-16): todoRevisions and reopenEvents added; every 2.0.0 field is unchanged. */
    public static final String SCHEMA_VERSION = "2.1.0";

    private final ExportMapper exportMapper;
    private final CurrentUserProvider currentUserProvider;
    private final SeoulDates seoulDates;
    private final StatementBudget statementBudget;

    public ExportService(ExportMapper exportMapper, CurrentUserProvider currentUserProvider, SeoulDates seoulDates,
                         StatementBudget statementBudget) {
        this.exportMapper = exportMapper;
        this.currentUserProvider = currentUserProvider;
        this.seoulDates = seoulDates;
        this.statementBudget = statementBudget;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Map<String, Object> export() {
        statementBudget.useAggregateTimeout();
        UUID userId = currentUserProvider.currentUserId();
        UserRow owner = exportMapper.owner(userId);
        if (owner == null) {
            throw new IllegalStateException("export owner is not an active user");
        }
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("schemaVersion", SCHEMA_VERSION);
        document.put("exportedAt", time(seoulDates.now().atOffset(ZoneOffset.UTC)));
        document.put("timezone", TimeConfig.SEOUL.getId());
        document.put("owner", owner(owner));
        document.put("plans", map(exportMapper.plans(userId), ExportService::plan));
        document.put("planRevisions", map(exportMapper.planRevisions(userId), ExportService::planRevision));
        document.put("todos", map(exportMapper.todos(userId), ExportService::todo));
        document.put("todoRevisions", map(exportMapper.todoRevisions(userId), ExportService::todoRevision));
        document.put("tags", map(exportMapper.tags(userId), ExportService::tag));
        document.put("todoTags", map(exportMapper.todoTags(userId), ExportService::todoTag));
        document.put("executionLogs", map(exportMapper.executionLogs(userId), ExportService::executionLog));
        document.put("completionEvents", map(exportMapper.completionEvents(userId), ExportService::completionEvent));
        document.put("reopenEvents", map(exportMapper.reopenEvents(userId), ExportService::reopenEvent));
        document.put("reviews", map(exportMapper.reviews(userId), ExportService::review));
        return document;
    }

    /** File name date is the Seoul calendar date of the export. */
    public String fileName() {
        return "plandosee-export-" + seoulDates.today().format(DateTimeFormatter.BASIC_ISO_DATE) + ".json";
    }

    private static Map<String, Object> owner(UserRow user) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id(user.getId()));
        m.put("nickname", user.getNickname());
        return m;
    }

    private static Map<String, Object> plan(PlanRow p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id(p.getId()));
        m.put("userId", id(p.getUserId()));
        m.put("title", p.getTitle());
        m.put("startDate", date(p.getStartDate()));
        m.put("endDate", date(p.getEndDate()));
        m.put("priority", p.getPriority().name());
        m.put("successCriteria", p.getSuccessCriteria());
        m.put("estimatedMinutes", p.getEstimatedMinutes());
        m.put("carriedImprovement", p.getCarriedImprovement());
        m.put("createdAt", time(p.getCreatedAt()));
        m.put("updatedAt", time(p.getUpdatedAt()));
        return m;
    }

    private static Map<String, Object> planRevision(PlanRevisionRow r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id(r.getId()));
        m.put("planId", id(r.getPlanId()));
        m.put("revisionNo", r.getRevisionNo());
        m.put("title", r.getTitle());
        m.put("startDate", date(r.getStartDate()));
        m.put("endDate", date(r.getEndDate()));
        m.put("priority", r.getPriority().name());
        m.put("successCriteria", r.getSuccessCriteria());
        m.put("estimatedMinutes", r.getEstimatedMinutes());
        m.put("carriedImprovement", r.getCarriedImprovement());
        m.put("revisedAt", time(r.getRevisedAt()));
        return m;
    }

    private static Map<String, Object> todo(TodoRow t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id(t.getId()));
        m.put("planId", id(t.getPlanId()));
        m.put("title", t.getTitle());
        m.put("dueDate", date(t.getDueDate()));
        m.put("priority", t.getPriority().name());
        m.put("estimatedMinutes", t.getEstimatedMinutes());
        m.put("status", t.getStatus().name());
        m.put("completionCycle", t.getCompletionCycle());
        m.put("completedAt", time(t.getCompletedAt()));
        m.put("createdAt", time(t.getCreatedAt()));
        m.put("updatedAt", time(t.getUpdatedAt()));
        return m;
    }

    private static Map<String, Object> todoRevision(TodoRevisionRow r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id(r.getId()));
        m.put("todoId", id(r.getTodoId()));
        m.put("revisionNo", r.getRevisionNo());
        m.put("title", r.getTitle());
        m.put("dueDate", date(r.getDueDate()));
        m.put("priority", r.getPriority().name());
        m.put("estimatedMinutes", r.getEstimatedMinutes());
        m.put("tagNames", List.copyOf(r.getTagNames()));
        m.put("revisedAt", time(r.getRevisedAt()));
        return m;
    }

    private static Map<String, Object> reopenEvent(ReopenEventRow e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id(e.getId()));
        m.put("todoId", id(e.getTodoId()));
        m.put("cycleNo", e.getCycleNo());
        m.put("reopenedAt", time(e.getReopenedAt()));
        m.put("createdAt", time(e.getCreatedAt()));
        return m;
    }

    private static Map<String, Object> tag(ExportTagRow g) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id(g.getId()));
        m.put("userId", id(g.getUserId()));
        m.put("name", g.getName());
        m.put("normalizedName", g.getNormalizedName());
        m.put("createdAt", time(g.getCreatedAt()));
        m.put("updatedAt", time(g.getUpdatedAt()));
        return m;
    }

    private static Map<String, Object> todoTag(ExportTodoTagRow tt) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("todoId", id(tt.getTodoId()));
        m.put("tagId", id(tt.getTagId()));
        m.put("createdAt", time(tt.getCreatedAt()));
        return m;
    }

    private static Map<String, Object> executionLog(ExecutionLogRow e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id(e.getId()));
        m.put("todoId", id(e.getTodoId()));
        m.put("startedAt", time(e.getStartedAt()));
        m.put("endedAt", time(e.getEndedAt()));
        m.put("actualMinutes", e.getActualMinutes());
        m.put("blockerReason", e.getBlockerReason());
        m.put("createdAt", time(e.getCreatedAt()));
        return m;
    }

    private static Map<String, Object> completionEvent(CompletionEventRow c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id(c.getId()));
        m.put("todoId", id(c.getTodoId()));
        m.put("idempotencyKey", id(c.getIdempotencyKey()));
        m.put("cycleNo", c.getCycleNo());
        m.put("completedAt", time(c.getCompletedAt()));
        m.put("createdAt", time(c.getCreatedAt()));
        return m;
    }

    private static Map<String, Object> review(ReviewRow r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id(r.getId()));
        m.put("userId", id(r.getUserId()));
        m.put("planId", id(r.getPlanId()));
        m.put("periodStart", date(r.getPeriodStart()));
        m.put("periodEnd", date(r.getPeriodEnd()));
        m.put("improvement", r.getImprovement());
        m.put("nextPlanId", id(r.getNextPlanId()));
        m.put("transferredAt", time(r.getTransferredAt()));
        m.put("createdAt", time(r.getCreatedAt()));
        m.put("updatedAt", time(r.getUpdatedAt()));
        return m;
    }

    private static <T> List<Map<String, Object>> map(List<T> rows, Function<T, Map<String, Object>> mapper) {
        return rows.stream().map(mapper).toList();
    }

    private static String id(UUID value) {
        return value == null ? null : value.toString();
    }

    private static String date(LocalDate value) {
        return value == null ? null : value.toString();
    }

    private static String time(OffsetDateTime value) {
        return value == null ? null
                : value.atZoneSameInstant(TimeConfig.SEOUL).toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
