package com.psycorp.psychapi.feature.test.models;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.bson.types.ObjectId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection = "review_tasks")
public class ReviewTask extends PanacheMongoEntity {
    private ObjectId resultId;
    private ObjectId attemptId;
    private ObjectId workspaceId;
    private ReviewReason reason;
    private TaskStatus status = TaskStatus.OPEN;
    private ObjectId assigneeId;
    private Integer priority = 0;
    private Integer slaHours = 48;
    private Instant dueAt;
    private Instant claimedAt;
    private Instant completedAt;
    private List<Map<String, Object>> escalationHistory;

    public enum ReviewReason {
        PROJECTIVE_TEST("PROJECTIVE_TEST"), RESTRICTED_TEST("RESTRICTED_TEST"),
        PURCHASED_REVIEW("PURCHASED_REVIEW"), AI_DRAFT_REVIEW("AI_DRAFT_REVIEW");
        private final String v; ReviewReason(String v) { this.v = v; }
        @JsonValue public String getValue() { return v; }
        @JsonCreator public static ReviewReason fromValue(String v) {
            for (ReviewReason val : values()) if (val.v.equalsIgnoreCase(v)) return val;
            return null;
        }
    }

    public enum TaskStatus {
        OPEN("OPEN"), CLAIMED("CLAIMED"), IN_REVIEW("IN_REVIEW"), DONE("DONE"), ESCALATED("ESCALATED"), CANCELLED("CANCELLED");
        private final String v; TaskStatus(String v) { this.v = v; }
        @JsonValue public String getValue() { return v; }
        @JsonCreator public static TaskStatus fromValue(String v) {
            for (TaskStatus val : values()) if (val.v.equalsIgnoreCase(v)) return val;
            return null;
        }
    }

    public ObjectId getResultId() { return resultId; }
    public void setResultId(ObjectId resultId) { this.resultId = resultId; }
    public ObjectId getAttemptId() { return attemptId; }
    public void setAttemptId(ObjectId attemptId) { this.attemptId = attemptId; }
    public ObjectId getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(ObjectId workspaceId) { this.workspaceId = workspaceId; }
    public ReviewReason getReason() { return reason; }
    public void setReason(ReviewReason reason) { this.reason = reason; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public ObjectId getAssigneeId() { return assigneeId; }
    public void setAssigneeId(ObjectId assigneeId) { this.assigneeId = assigneeId; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public Integer getSlaHours() { return slaHours; }
    public void setSlaHours(Integer slaHours) { this.slaHours = slaHours; }
    public Instant getDueAt() { return dueAt; }
    public void setDueAt(Instant dueAt) { this.dueAt = dueAt; }
    public Instant getClaimedAt() { return claimedAt; }
    public void setClaimedAt(Instant claimedAt) { this.claimedAt = claimedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public List<Map<String, Object>> getEscalationHistory() { return escalationHistory; }
    public void setEscalationHistory(List<Map<String, Object>> escalationHistory) { this.escalationHistory = escalationHistory; }
}