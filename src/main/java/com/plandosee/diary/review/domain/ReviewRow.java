package com.plandosee.diary.review.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public class ReviewRow {

    private UUID id;
    private UUID userId;
    private UUID planId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String improvement;
    private UUID nextPlanId;
    private OffsetDateTime transferredAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    /** True once the improvement was carried to a next plan; the improvement is then read-only (ADR-08). */
    public boolean isTransferred() {
        return nextPlanId != null;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getPlanId() {
        return planId;
    }

    public void setPlanId(UUID planId) {
        this.planId = planId;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public String getImprovement() {
        return improvement;
    }

    public void setImprovement(String improvement) {
        this.improvement = improvement;
    }

    public UUID getNextPlanId() {
        return nextPlanId;
    }

    public void setNextPlanId(UUID nextPlanId) {
        this.nextPlanId = nextPlanId;
    }

    public OffsetDateTime getTransferredAt() {
        return transferredAt;
    }

    public void setTransferredAt(OffsetDateTime transferredAt) {
        this.transferredAt = transferredAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
