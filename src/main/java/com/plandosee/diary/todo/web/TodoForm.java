package com.plandosee.diary.todo.web;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;

import com.plandosee.diary.common.domain.Priority;
import com.plandosee.diary.todo.application.TagNames;
import com.plandosee.diary.todo.application.TodoCommand;
import com.plandosee.diary.todo.domain.TagRow;
import com.plandosee.diary.todo.domain.TodoRow;
import com.plandosee.diary.todo.domain.TodoRules;

/**
 * Todo create/edit form. Only the ADR-07 editable fields exist here; status, plan, and id cannot be submitted.
 * tags is a comma-separated list parsed by TagNames (each 1..TodoRules.TAG_NAME_MAX chars, case-insensitive
 * duplicates merged). Limits come from TodoRules (ADR-22).
 */
public class TodoForm {

    @NotBlank(message = "{validation.title.required}")
    @Size(max = TodoRules.TITLE_MAX, message = "{validation.title.max}")
    private String title;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDate;

    @NotNull(message = "{validation.priority.required}")
    private Priority priority;

    @NotNull(message = "{validation.estimatedMinutes.required}")
    @Min(value = TodoRules.ESTIMATED_MINUTES_MIN, message = "{validation.estimatedMinutes.min}")
    @Max(value = TodoRules.ESTIMATED_MINUTES_MAX, message = "{validation.estimatedMinutes.max}")
    private Integer estimatedMinutes;

    private String tags;

    private Integer version;

    public static TodoForm from(TodoRow todo) {
        TodoForm form = new TodoForm();
        form.setTitle(todo.getTitle());
        form.setDueDate(todo.getDueDate());
        form.setPriority(todo.getPriority());
        form.setEstimatedMinutes(todo.getEstimatedMinutes());
        form.setTags(TagNames.join(todo.getTags().stream().map(TagRow::getName).toList()));
        form.setVersion(todo.getVersion());
        return form;
    }

    /** May throw DomainRuleException("tags", ...) for an over-long tag. */
    public TodoCommand toCommand() {
        return new TodoCommand(title, dueDate, priority, estimatedMinutes, TagNames.parse(tags));
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public Integer getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(Integer estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    /**
     * ADR-18 hidden field: the version the edit form was opened with (after a stale-version conflict, the latest
     * version). Only compared to detect an out-of-date save; never an authorization value. Absent: no check.
     */
    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
