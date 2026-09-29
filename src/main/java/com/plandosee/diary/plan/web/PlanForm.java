package com.plandosee.diary.plan.web;

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
import com.plandosee.diary.plan.application.PlanCommand;
import com.plandosee.diary.plan.domain.PlanRow;
import com.plandosee.diary.plan.domain.PlanRules;

/**
 * Plan create/edit/next-plan form (web-contract.md). Strings arrive trimmed; blank becomes null (FormBindingAdvice).
 * Limits come from PlanRules (ADR-22), the same values PlanService checks and the DB CHECK constraints hold.
 * endDate >= startDate is the PlanPeriod rule, checked here as a form-level constraint and shown on the endDate
 * field together with the other field errors (QA-D5); PlanService checks it again for direct calls.
 * The estimated time is three boxes combined into whole minutes (ADR-29, {@link EstimatedDurationForm}); every error
 * of the group is on the field {@code estimatedMinutes}. Dates are parsed and range-checked while binding
 * (FormBindingAdvice, code validation.date.outOfRange). Text content (line breaks, control characters) is
 * {@code @PlainText}; PlanCommand checks everything again (ADR-30).
 */
@ValidPlanPeriod
@ValidEstimatedDuration
public class PlanForm implements EstimatedDurationForm {

    @NotBlank(message = "{validation.title.required}")
    @Size(max = PlanRules.TITLE_MAX, message = "{validation.title.max}")
    @PlainText(TextInput.Lines.SINGLE)
    private String title;

    @NotNull(message = "{validation.startDate.required}")
    private LocalDate startDate;

    @NotNull(message = "{validation.endDate.required}")
    private LocalDate endDate;

    @NotNull(message = "{validation.priority.required}")
    private Priority priority;

    @NotBlank(message = "{validation.successCriteria.required}")
    @Size(max = PlanRules.SUCCESS_CRITERIA_MAX, message = "{validation.successCriteria.max}")
    @PlainText(TextInput.Lines.MULTI)
    private String successCriteria;

    private String estimatedDays;

    private String estimatedHours;

    private String estimatedMinutesPart;

    private Integer version;

    public static PlanForm from(PlanRow plan) {
        PlanForm form = new PlanForm();
        form.setTitle(plan.getTitle());
        form.setStartDate(plan.getStartDate());
        form.setEndDate(plan.getEndDate());
        form.setPriority(plan.getPriority());
        form.setSuccessCriteria(plan.getSuccessCriteria());
        form.fillEstimatedMinutes(plan.getEstimatedMinutes());
        form.setVersion(plan.getVersion());
        return form;
    }

    /**
     * Called after validation passed. A broken estimated-time rule still throws DomainRuleException with the group
     * code rather than sending a made-up value to the service.
     */
    public PlanCommand toCommand() {
        DurationInput.Result estimated = estimatedInput();
        if (!estimated.valid()) {
            throw new DomainRuleException(FIELD, estimated.code());
        }
        return new PlanCommand(title, startDate, endDate, priority, successCriteria, estimated.totalMinutes());
    }

    /** Fills the three boxes from whole minutes (not a bean setter, so no request parameter can reach it). */
    public PlanForm fillEstimatedMinutes(int minutes) {
        String[] boxes = EstimatedDurationForm.boxes(minutes);
        this.estimatedDays = boxes[0];
        this.estimatedHours = boxes[1];
        this.estimatedMinutesPart = boxes[2];
        return this;
    }

    @Override
    public int estimatedMinutesMax() {
        return PlanRules.ESTIMATED_MINUTES_MAX;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getSuccessCriteria() {
        return successCriteria;
    }

    public void setSuccessCriteria(String successCriteria) {
        this.successCriteria = successCriteria;
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
