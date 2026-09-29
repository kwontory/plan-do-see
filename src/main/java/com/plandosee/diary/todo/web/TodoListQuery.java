package com.plandosee.diary.todo.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.util.UriComponentsBuilder;

import com.plandosee.diary.common.domain.Priority;
import com.plandosee.diary.common.domain.TextInput;
import com.plandosee.diary.common.domain.UuidText;
import com.plandosee.diary.common.error.FieldViolation;
import com.plandosee.diary.common.paging.PageRequest;
import com.plandosee.diary.todo.domain.DueFilter;
import com.plandosee.diary.todo.domain.TodoRules;
import com.plandosee.diary.todo.domain.TodoSort;
import com.plandosee.diary.todo.domain.TodoStatus;

/**
 * S02 search/filter/sort state (ADR-06) and page (ADR-21). Bound from the query string on GET and from hidden fields
 * on list POSTs (web-contract revision 1 Q10). Unknown values are dropped (sort falls back to DUE, page to 1), so
 * only allowlisted values ever reach SQL or a redirect URL. The search text is checked with TodoRules.SEARCH_QUERY
 * (ADR-30 revised, IV-04): a rejected text is kept as typed so the list can show it with a field error, and it is
 * never searched ({@link #searchViolations()}; the service's TodoFilter applies the same rule). The tag id must be a
 * canonical UUID (UuidText).
 */
public class TodoListQuery {

    /**
     * Longest search text a list URL carries (redirects, page links): twice SEARCH_QUERY_MAX. A valid text and a
     * rejected one of up to this length are carried as they are, so the next list shows the same search or the same
     * rejection; a longer text (only from a hand-made request, the search box has maxlength) is cut to this length
     * and is still over the limit. 100 Hangul characters are 900 bytes percent-encoded, far below the 8 KB header
     * buffer (IV-04).
     */
    static final int CARRIED_QUERY_MAX = TodoRules.SEARCH_QUERY_MAX * 2;

    private String q;
    private String status;
    private String priority;
    private String tagId;
    private String due;
    private String sort;
    private String page;

    /** A copy with every value parsed against its allowlist and rendered back in canonical form. */
    public TodoListQuery normalized() {
        TodoListQuery n = new TodoListQuery();
        n.q = TextInput.normalize(q);
        n.status = statusValue() == null ? null : statusValue().name();
        n.priority = priorityValue() == null ? null : priorityValue().name();
        n.tagId = tagIdValue() == null ? null : tagIdValue().toString();
        n.due = dueValue() == null ? null : dueValue().name();
        n.sort = sortValue().name();
        n.page = String.valueOf(pageValue());
        return n;
    }

    /**
     * /plans/{planId}/todos with only the allowlisted list state and the current page (omitted when 1); every value
     * is percent-encoded. Used for redirects after add, complete, reopen, and delete (ADR-21: the page is kept; a
     * page that no longer exists is shown as the last page by the list itself).
     */
    public String listUrl(UUID planId) {
        return listUrl(planId, pageValue());
    }

    /** The same list state on another page (page links, ADR-21). */
    public String listUrl(UUID planId, int page) {
        TodoListQuery n = normalized();
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("planId", planId);
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/plans/{planId}/todos");
        addParam(builder, vars, "q", TextInput.truncate(n.q, CARRIED_QUERY_MAX));
        addParam(builder, vars, "status", n.status);
        addParam(builder, vars, "priority", n.priority);
        addParam(builder, vars, "tagId", n.tagId);
        addParam(builder, vars, "due", n.due);
        if (n.sort != null && !TodoSort.DUE.name().equals(n.sort)) {
            addParam(builder, vars, "sort", n.sort);
        }
        if (page > PageRequest.FIRST) {
            addParam(builder, vars, "page", String.valueOf(page));
        }
        return builder.encode().buildAndExpand(vars).toUriString();
    }

    private static void addParam(UriComponentsBuilder builder, Map<String, Object> vars, String name, String value) {
        if (value != null) {
            builder.queryParam(name, "{" + name + "}");
            vars.put(name, value);
        }
    }

    /**
     * The broken rules of the search text (TodoRules.SEARCH_QUERY, field "q"); empty when the list may be searched.
     */
    public List<FieldViolation> searchViolations() {
        return TodoRules.searchQueryViolations(q);
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

    /** Requested page, 1 when absent or unreadable (PageRequest.parse). */
    public int pageValue() {
        return PageRequest.parse(page);
    }

    public UUID tagIdValue() {
        if (tagId == null || tagId.isBlank()) {
            return null;
        }
        return UuidText.parse(tagId.strip());
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

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }
}
