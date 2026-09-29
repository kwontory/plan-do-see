package com.plandosee.diary.todo.domain;

import java.time.LocalDate;
import java.util.UUID;

import com.plandosee.diary.common.domain.Priority;

/**
 * Server-built query object. userId always comes from CurrentUserProvider, never from the request. The search text
 * is normalized and checked with TodoRules.SEARCH_QUERY: a filter with a rejected text (longer than
 * SEARCH_QUERY_MAX, line break, control character) cannot be built; the constructor throws
 * DomainRuleException(TodoRules.SEARCH_FIELD, code) before anything is read.
 */
public class TodoFilter {

    private final UUID userId;
    private final UUID planId;
    private final String query;
    private final TodoStatus status;
    private final Priority priority;
    private final UUID tagId;
    private final DueFilter due;
    private final TodoSort sort;
    private final LocalDate today;
    private final Integer limit;
    private final Long offset;

    public TodoFilter(UUID userId, UUID planId, String query, TodoStatus status, Priority priority,
                      UUID tagId, DueFilter due, TodoSort sort, LocalDate today) {
        this(userId, planId, query, status, priority, tagId, due, sort, today, null, null);
    }

    private TodoFilter(UUID userId, UUID planId, String query, TodoStatus status, Priority priority,
                       UUID tagId, DueFilter due, TodoSort sort, LocalDate today, Integer limit, Long offset) {
        this.limit = limit;
        this.offset = offset;
        this.userId = userId;
        this.planId = planId;
        this.query = TodoRules.searchQuery(query);
        this.status = status;
        this.priority = priority;
        this.tagId = tagId;
        this.due = due;
        this.sort = sort == null ? TodoSort.DUE : sort;
        this.today = today;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getPlanId() {
        return planId;
    }

    public String getQuery() {
        return query;
    }

    /** ILIKE pattern body with \, %, _ escaped (ESCAPE '\'). */
    public String getQueryPattern() {
        if (query == null) {
            return null;
        }
        return query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public TodoStatus getStatus() {
        return status;
    }

    public Priority getPriority() {
        return priority;
    }

    public UUID getTagId() {
        return tagId;
    }

    public DueFilter getDue() {
        return due;
    }

    public String getDueKey() {
        return due == null ? null : due.name();
    }

    public TodoSort getSort() {
        return sort;
    }

    public String getSortKey() {
        return sort.name();
    }

    public LocalDate getToday() {
        return today;
    }

    /** The same conditions and order, narrowed to one page. */
    public TodoFilter page(int limit, long offset) {
        return new TodoFilter(userId, planId, query, status, priority, tagId, due, sort, today, limit, offset);
    }

    /** SQL LIMIT, or null for the whole list. */
    public Integer getLimit() {
        return limit;
    }

    /** SQL OFFSET, or null for the whole list. */
    public Long getOffset() {
        return offset;
    }
}
