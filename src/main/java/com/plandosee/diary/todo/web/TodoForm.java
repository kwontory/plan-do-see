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

/**
 * Todo create/edit form. Only the ADR-07 editable fields exist here; status, plan, and id cannot be submitted.
 * tags is a comma-separated list parsed by TagNames (each 1..50 chars, case-insensitive duplicates merged).
 */
public class TodoForm {

    @NotBlank(message = "{validation.title.required}")
    @Size(max = 200, message = "{validation.title.max}")
    private String title;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDate;

    @NotNull(message = "{validation.priority.required}")
    private Priority priority;

    @NotNull(message = "{validation.estimatedMinutes.required}")
    @Min(value = 0, message = "{validation.estimatedMinutes.min}")
    @Max(value = 525600, message = "{validation.estimatedMinutes.max}")
    private Integer estimatedMinutes;

    private String tags;

    public static TodoForm from(TodoRow todo) {
        TodoForm form = new TodoForm();
        form.setTitle(todo.getTitle());
        form.setDueDate(todo.getDueDate());
        form.setPriority(todo.getPriority());
        form.setEstimatedMinutes(todo.getEstimatedMinutes());
        form.setTags(TagNames.join(todo.getTags().stream().map(TagRow::getName).toList()));
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
}
