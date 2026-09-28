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
 * Plan create/edit form (web-contract.md). Strings arrive trimmed; blank becomes null (FormBindingAdvice).
 * endDate >= startDate is a domain rule checked by PlanService and shown on the endDate field.
 */
public class PlanForm {

    @NotBlank(message = "제목을 입력하세요.")
    @Size(max = 200, message = "제목은 200자 이하로 입력하세요.")
    private String title;

    @NotNull(message = "시작일을 입력하세요.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @NotNull(message = "종료일을 입력하세요.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @NotNull(message = "우선순위를 선택하세요.")
    private Priority priority;

    @NotBlank(message = "성공 기준을 입력하세요.")
    @Size(max = 1000, message = "성공 기준은 1000자 이하로 입력하세요.")
    private String successCriteria;

    @NotNull(message = "예상 시간(분)을 입력하세요.")
    @Min(value = 0, message = "예상 시간은 0분 이상이어야 합니다.")
    @Max(value = 525600, message = "예상 시간은 525600분 이하여야 합니다.")
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
