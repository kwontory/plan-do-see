package com.plandosee.diary.execution.web;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;

import com.plandosee.diary.common.time.TimeConfig;
import com.plandosee.diary.execution.domain.ExecutionRules;

/**
 * S03 execution record form. datetime-local values (with or without seconds) are interpreted in Asia/Seoul.
 * blockerReason is optional (at most ExecutionRules.BLOCKER_REASON_MAX characters, LF line breaks, ADR-22); blank is
 * stored as NULL by ExecutionService.
 */
public class ExecutionForm {

    @NotNull(message = "{validation.startedAt.required}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startedAt;

    @NotNull(message = "{validation.endedAt.required}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endedAt;

    @Size(max = ExecutionRules.BLOCKER_REASON_MAX, message = "{validation.blockerReason.max}")
    private String blockerReason;

    public static OffsetDateTime inSeoul(LocalDateTime value) {
        return value == null ? null : value.atZone(TimeConfig.SEOUL).toOffsetDateTime();
    }

    public OffsetDateTime startedAtInSeoul() {
        return inSeoul(startedAt);
    }

    public OffsetDateTime endedAtInSeoul() {
        return inSeoul(endedAt);
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public String getBlockerReason() {
        return blockerReason;
    }

    public void setBlockerReason(String blockerReason) {
        this.blockerReason = blockerReason;
    }
}
