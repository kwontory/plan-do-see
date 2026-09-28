package com.plandosee.diary.todo.web;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.util.UriComponentsBuilder;

import com.plandosee.diary.common.domain.Priority;
import com.plandosee.diary.todo.domain.DueFilter;
import com.plandosee.diary.todo.domain.TodoSort;
import com.plandosee.diary.todo.domain.TodoStatus;

/**
 * S02 search/filter/sort state (ADR-06). Bound from the query string on GET and from hidden fields on list POSTs
 * (web-contract revision 1 Q10). Unknown values are dropped (sort falls back to DUE), so only allowlisted values
 * ever reach SQL or a redirect URL.
 */
public class TodoListQuery {

    private String q;
    private String status;
    private String priority;
    private String tagId;
    private String due;
    private String sort;

    /** A copy with every value parsed against its allowlist and rendered back in canonical form. */
    public TodoListQuery normalized() {
        TodoListQuery n = new TodoListQuery();
        n.q = q == null || q.isBlank() ? null : q.strip();
        n.status = statusValue() == null ? null : statusValue().name();
        n.priority = priorityValue() == null ? null : priorityValue().name();
        n.tagId = tagIdValue() == null ? null : tagIdValue().toString();
        n.due = dueValue() == null ? null : dueValue().name();
        n.sort = sortValue().name();
        return n;
    }

    /** /plans/{planId}/todos with only the allowlisted list state; every value is percent-encoded. */
    public String listUrl(UUID planId) {
        TodoListQuery n = normalized();
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("planId", planId);
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/plans/{planId}/todos");
        addParam(builder, vars, "q", n.q);
        addParam(builder, vars, "status", n.status);
        addParam(builder, vars, "priority", n.priority);
        addParam(builder, vars, "tagId", n.tagId);
        addParam(builder, vars, "due", n.due);
        if (n.sort != null && !TodoSort.DUE.name().equals(n.sort)) {
            addParam(builder, vars, "sort", n.sort);
        }
        return builder.encode().buildAndExpand(vars).toUriString();
    }

    private static void addParam(UriComponentsBuilder builder, Map<String, Object> vars, String name, String value) {
        if (value != null) {
            builder.queryParam(name, "{" + name + "}");
            vars.put(name, value);
        }
    }

    public TodoStatus statusValue() {
        return parseEnum(TodoStatus.class, status);
    }

    public Priority priorityValue() {
        return parseEnum(Priority.class, priority);
    }

    public DueFilter dueValue() {
        return parseEnum(DueFilter.class, due);
    }

    public TodoSort sortValue() {
        return TodoSort.fromParam(sort);
    }

    public UUID tagIdValue() {
        if (tagId == null || tagId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(tagId.strip());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        if (value == null) {
            return null;
        }
        for (E constant : type.getEnumConstants()) {
            if (constant.name().equals(value.strip())) {
                return constant;
            }
        }
        return null;
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getTagId() {
        return tagId;
    }

    public void setTagId(String tagId) {
        this.tagId = tagId;
    }

    public String getDue() {
        return due;
    }

    public void setDue(String due) {
        this.due = due;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }
}
