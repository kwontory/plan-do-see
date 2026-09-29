package com.plandosee.diary.plan.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.plandosee.diary.common.domain.DurationParts;
import com.plandosee.diary.common.domain.Priority;

public class PlanRevisionRow {

    private UUID id;
    private UUID planId;
    private int revisionNo;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private Priority priority;
    private String successCriteria;
    private int estimatedMinutes;
    private String carriedImprovement;
    private OffsetDateTime revisedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPlanId() {
        return planId;
    }

    public void setPlanId(UUID planId) {
        this.planId = planId;
    }

    public int getRevisionNo() {
        return revisionNo;
    }

    public void setRevisionNo(int revisionNo) {
        this.revisionNo = revisionNo;
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

    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(int estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    /** estimatedMinutes split into days, hours and minutes for display (not stored, not exported). */
    public DurationParts getEstimatedDuration() {
        return DurationParts.of(estimatedMinutes);
    }

    public String getCarriedImprovement() {
        return carriedImprovement;
    }

    public void setCarriedImprovement(String carriedImprovement) {
        this.carriedImprovement = carriedImprovement;
    }

    public OffsetDateTime getRevisedAt() {
        return revisedAt;
    }

    public void setRevisedAt(OffsetDateTime revisedAt) {
        this.revisedAt = revisedAt;
    }
}
