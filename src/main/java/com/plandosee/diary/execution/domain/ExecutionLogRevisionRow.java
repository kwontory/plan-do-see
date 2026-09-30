package com.plandosee.diary.execution.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

/** The values of an execution record just before one edit (ADR-40). Stored and exported, not shown on screen. */
public class ExecutionLogRevisionRow {

    private UUID id;
    private UUID executionLogId;
    private int revisionNo;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private int actualMinutes;
    private String blockerReason;
    private OffsetDateTime revisedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getExecutionLogId() { return executionLogId; }
    public void setExecutionLogId(UUID executionLogId) { this.executionLogId = executionLogId; }
    public int getRevisionNo() { return revisionNo; }
    public void setRevisionNo(int revisionNo) { this.revisionNo = revisionNo; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }
    public OffsetDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(OffsetDateTime endedAt) { this.endedAt = endedAt; }
    public int getActualMinutes() { return actualMinutes; }
    public void setActualMinutes(int actualMinutes) { this.actualMinutes = actualMinutes; }
    public String getBlockerReason() { return blockerReason; }
    public void setBlockerReason(String blockerReason) { this.blockerReason = blockerReason; }
    public OffsetDateTime getRevisedAt() { return revisedAt; }
    public void setRevisedAt(OffsetDateTime revisedAt) { this.revisedAt = revisedAt; }
}
