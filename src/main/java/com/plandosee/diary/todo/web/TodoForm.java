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

    @NotBlank(message = "제목을 입력하세요.")
    @Size(max = 200, message = "제목은 200자 이하로 입력하세요.")
    private String title;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDate;

    @NotNull(message = "우선순위를 선택하세요.")
    private Priority priority;

    @NotNull(message = "예상 시간(분)을 입력하세요.")
    @Min(value = 0, message = "예상 시간은 0분 이상이어야 합니다.")
    @Max(value = 525600, message = "예상 시간은 525600분 이하여야 합니다.")
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
