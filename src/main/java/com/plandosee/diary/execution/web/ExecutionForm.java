package com.plandosee.diary.execution.web;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.plandosee.diary.common.domain.TextInput;
import com.plandosee.diary.common.time.TimeConfig;
import com.plandosee.diary.common.web.PlainText;
import com.plandosee.diary.execution.application.ExecutionCommand;
import com.plandosee.diary.execution.domain.ExecutionLogRow;
import com.plandosee.diary.execution.domain.ExecutionRules;

/**
 * Execution record form. datetime-local values (with or without seconds, never with an offset: FormBindingAdvice)
 * are interpreted in Asia/Seoul.
 * blockerReason is optional (at most ExecutionRules.BLOCKER_REASON_MAX characters, LF line breaks); blank is
 * stored as NULL by ExecutionService.
 */
public class ExecutionForm {

    @NotNull(message = "{validation.startedAt.required}")
    private LocalDateTime startedAt;

    @NotNull(message = "{validation.endedAt.required}")
    private LocalDateTime endedAt;

    @Size(max = ExecutionRules.BLOCKER_REASON_MAX, message = "{validation.blockerReason.max}")
    @PlainText(TextInput.Lines.MULTI)
    private String blockerReason;

    /** Hidden edit version (edit form only, ADR-18); null when adding a record. */
    private Integer version;

    /** The edit form's values from a stored record (times in Asia/Seoul). */
    public static ExecutionForm from(ExecutionLogRow log) {
        ExecutionForm form = new ExecutionForm();
        form.setStartedAt(log.getStartedAt().atZoneSameInstant(TimeConfig.SEOUL).toLocalDateTime());
        form.setEndedAt(log.getEndedAt().atZoneSameInstant(TimeConfig.SEOUL).toLocalDateTime());
        form.setBlockerReason(log.getBlockerReason());
        form.setVersion(log.getVersion());
        return form;
    }

    /** The command of this input (checks every rule; DomainRuleException when one is broken). */
    public ExecutionCommand toCommand() {
        return new ExecutionCommand(startedAtInSeoul(), endedAtInSeoul(), blockerReason);
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

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
