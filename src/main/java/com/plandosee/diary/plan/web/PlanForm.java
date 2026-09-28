package com.plandosee.diary.plan.web;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;

import com.plandosee.diary.common.domain.Priority;
import com.plandosee.diary.plan.application.PlanCommand;
import com.plandosee.diary.plan.domain.PlanRow;

/**
 * Plan create/edit/next-plan form (web-contract.md). Strings arrive trimmed; blank becomes null (FormBindingAdvice).
 * endDate >= startDate is the PlanPeriod rule, checked here as a form-level constraint and shown on the endDate
 * field together with the other field errors (QA-D5); PlanService checks it again for direct calls.
 */
@ValidPlanPeriod
public class PlanForm {

    @NotBlank(message = "{validation.title.required}")
    @Size(max = 200, message = "{validation.title.max}")
    private String title;

    @NotNull(message = "{validation.startDate.required}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @NotNull(message = "{validation.endDate.required}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @NotNull(message = "{validation.priority.required}")
    private Priority priority;

    @NotBlank(message = "{validation.successCriteria.required}")
    @Size(max = 1000, message = "{validation.successCriteria.max}")
    private String successCriteria;

    @NotNull(message = "{validation.estimatedMinutes.required}")
    @Min(value = 0, message = "{validation.estimatedMinutes.min}")
    @Max(value = 525600, message = "{validation.estimatedMinutes.max}")
    private Integer estimatedMinutes;

    public static PlanForm from(PlanRow plan) {
        PlanForm form = new PlanForm();
        form.setTitle(plan.getTitle());
        form.setStartDate(plan.getStartDate());
        form.setEndDate(plan.getEndDate());
        form.setPriority(plan.getPriority());
        form.setSuccessCriteria(plan.getSuccessCriteria());
        form.setEstimatedMinutes(plan.getEstimatedMinutes());
        return form;
    }

    public PlanCommand toCommand() {
        return new PlanCommand(title, startDate, endDate, priority, successCriteria, estimatedMinutes);
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

    public Integer getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(Integer estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }
}
