package com.plandosee.diary.todo.web;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.plandosee.diary.common.domain.DurationInput;
import com.plandosee.diary.common.domain.DurationParts;
import com.plandosee.diary.common.domain.Priority;
import com.plandosee.diary.common.domain.TextInput;
import com.plandosee.diary.common.error.DomainRuleException;
import com.plandosee.diary.common.web.EstimatedDurationForm;
import com.plandosee.diary.common.web.PlainText;
import com.plandosee.diary.common.web.ValidEstimatedDuration;
import com.plandosee.diary.todo.application.TagNames;
import com.plandosee.diary.todo.application.TodoCommand;
import com.plandosee.diary.todo.domain.TagRow;
import com.plandosee.diary.todo.domain.TodoRow;
import com.plandosee.diary.todo.domain.TodoRules;

/**
 * Todo create/edit form. Only the editable fields exist here; status, plan, and id cannot be submitted.
 * tags is a comma-separated list parsed by TagNames (each 1..TodoRules.TAG_NAME_MAX chars, case-insensitive
 * duplicates merged, at most TodoRules.TAGS_MAX tags). Limits come from TodoRules.
 * The estimated time is three boxes combined into whole minutes ({@link EstimatedDurationForm}); every error
 * of the group is on the field {@code estimatedMinutes}. A due date outside DateBounds is rejected while binding
 * (FormBindingAdvice, code validation.date.outOfRange).
 */
@ValidEstimatedDuration
public class TodoForm implements EstimatedDurationForm {

    @NotBlank(message = "{validation.title.required}")
    @Size(max = TodoRules.TITLE_MAX, message = "{validation.title.max}")
    @PlainText(TextInput.Lines.SINGLE)
    private String title;

    private LocalDate dueDate;

    @NotNull(message = "{validation.priority.required}")
    private Priority priority;

    private String estimatedDays;

    private String estimatedHours;

    private String estimatedMinutesPart;

    private String tags;

    private Integer version;

    public static TodoForm from(TodoRow todo) {
        TodoForm form = new TodoForm();
        form.setTitle(todo.getTitle());
        form.setDueDate(todo.getDueDate());
        form.setPriority(todo.getPriority());
        form.fillEstimatedMinutes(todo.getEstimatedMinutes());
        form.setTags(TagNames.join(todo.getTags().stream().map(TagRow::getName).toList()));
        form.setVersion(todo.getVersion());
        return form;
    }

    /**
     * May throw DomainRuleException("tags", ...) for an over-long tag or too many tags, and the estimated-time group
     * code if the boxes break the rule (normally caught by validation first).
     */
    public TodoCommand toCommand() {
        DurationInput.Result estimated = estimatedInput();
        if (!estimated.valid()) {
            throw new DomainRuleException(FIELD, estimated.code());
        }
        return new TodoCommand(title, dueDate, priority, estimated.totalMinutes(), TagNames.parse(tags));
    }

    /** Fills the three boxes from whole minutes (not a bean setter, so no request parameter can reach it). */
    public TodoForm fillEstimatedMinutes(int minutes) {
        String[] boxes = EstimatedDurationForm.boxes(minutes);
        this.estimatedDays = boxes[0];
        this.estimatedHours = boxes[1];
        this.estimatedMinutesPart = boxes[2];
        return this;
    }

    @Override
    public int estimatedMinutesMax() {
        return TodoRules.ESTIMATED_MINUTES_MAX;
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

    @Override
    public String getEstimatedDays() {
        return estimatedDays;
    }

    public void setEstimatedDays(String estimatedDays) {
        this.estimatedDays = estimatedDays;
    }

    @Override
    public String getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(String estimatedHours) {
        this.estimatedHours = estimatedHours;
    }

    @Override
    public String getEstimatedMinutesPart() {
        return estimatedMinutesPart;
    }

    public void setEstimatedMinutesPart(String estimatedMinutesPart) {
        this.estimatedMinutesPart = estimatedMinutesPart;
    }

    /** Combined whole minutes, or null while the boxes break the rule. Read-only: the error field of the group. */
    public Integer getEstimatedMinutes() {
        return estimatedInput().totalMinutes();
    }

    /** Parts of the combined value for read-only display of the submitted input; null while invalid. */
    public DurationParts getEstimatedDuration() {
        Integer minutes = getEstimatedMinutes();
        return minutes == null ? null : DurationParts.of(minutes);
    }

    /** aria-invalid of the days box when the group has an error (the box breaks its rule, or the whole group does). */
    public boolean isEstimatedDaysInvalid() {
        return estimatedInput().invalid(DurationInput.Part.DAYS);
    }

    public boolean isEstimatedHoursInvalid() {
        return estimatedInput().invalid(DurationInput.Part.HOURS);
    }

    public boolean isEstimatedMinutesPartInvalid() {
        return estimatedInput().invalid(DurationInput.Part.MINUTES);
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    /**
     * Hidden field: the version the edit form was opened with (after a stale-version conflict, the latest
     * version). Only compared to detect an out-of-date save; never an authorization value. Absent: no check.
     */
    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
