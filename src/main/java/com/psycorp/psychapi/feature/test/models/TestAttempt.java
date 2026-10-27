package com.psycorp.psychapi.feature.test.models;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.bson.types.ObjectId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection = "test_attempts")
public class TestAttempt extends PanacheMongoEntity {
    private ObjectId assignmentId;
    private ObjectId workspaceId;
    private ObjectId participantId;
    private ObjectId testId;
    private String testVersion;
    private Integer attemptNumber = 1;
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;
    private ObjectId currentSectionId;
    private List<Map<String, Object>> sectionStates;
    private Instant startedAt = Instant.now();
    private Instant completedAt;
    private Instant lastActivityAt = Instant.now();
    private List<Map<String, Object>> pauseLog;
    private Map<String, Object> clientInfo;
    private List<Map<String, Object>> anomalyFlags;
    private Boolean proctored = false;
    private ScoringStatus scoringStatus = ScoringStatus.PENDING;

    public enum AttemptStatus {
        IN_PROGRESS("IN_PROGRESS"), COMPLETED("COMPLETED"), ABANDONED("ABANDONED"), EXPIRED("EXPIRED");
        private final String v; AttemptStatus(String v) { this.v = v; }
        @JsonValue public String getValue() { return v; }
        @JsonCreator public static AttemptStatus fromValue(String v) {
            for (AttemptStatus val : values()) if (val.v.equalsIgnoreCase(v)) return val;
            return null;
        }
    }

    public enum ScoringStatus {
        PENDING("PENDING"), RUNNING("RUNNING"), DONE("DONE"), FAILED("FAILED"), NOT_APPLICABLE("NOT_APPLICABLE");
        private final String v; ScoringStatus(String v) { this.v = v; }
        @JsonValue public String getValue() { return v; }
        @JsonCreator public static ScoringStatus fromValue(String v) {
            for (ScoringStatus val : values()) if (val.v.equalsIgnoreCase(v)) return val;
            return null;
        }
    }

    public ObjectId getAssignmentId() { return assignmentId; }
    public void setAssignmentId(ObjectId assignmentId) { this.assignmentId = assignmentId; }
    public ObjectId getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(ObjectId workspaceId) { this.workspaceId = workspaceId; }
    public ObjectId getParticipantId() { return participantId; }
    public void setParticipantId(ObjectId participantId) { this.participantId = participantId; }
    public ObjectId getTestId() { return testId; }
    public void setTestId(ObjectId testId) { this.testId = testId; }
    public String getTestVersion() { return testVersion; }
    public void setTestVersion(String testVersion) { this.testVersion = testVersion; }
    public Integer getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(Integer attemptNumber) { this.attemptNumber = attemptNumber; }
    public AttemptStatus getStatus() { return status; }
    public void setStatus(AttemptStatus status) { this.status = status; }
    public ObjectId getCurrentSectionId() { return currentSectionId; }
    public void setCurrentSectionId(ObjectId currentSectionId) { this.currentSectionId = currentSectionId; }
    public List<Map<String, Object>> getSectionStates() { return sectionStates; }
    public void setSectionStates(List<Map<String, Object>> sectionStates) { this.sectionStates = sectionStates; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Instant getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(Instant lastActivityAt) { this.lastActivityAt = lastActivityAt; }
    public List<Map<String, Object>> getPauseLog() { return pauseLog; }
    public void setPauseLog(List<Map<String, Object>> pauseLog) { this.pauseLog = pauseLog; }
    public Map<String, Object> getClientInfo() { return clientInfo; }
    public void setClientInfo(Map<String, Object> clientInfo) { this.clientInfo = clientInfo; }
    public List<Map<String, Object>> getAnomalyFlags() { return anomalyFlags; }
    public void setAnomalyFlags(List<Map<String, Object>> anomalyFlags) { this.anomalyFlags = anomalyFlags; }
    public Boolean getProctored() { return proctored; }
    public void setProctored(Boolean proctored) { this.proctored = proctored; }
    public ScoringStatus getScoringStatus() { return scoringStatus; }
    public void setScoringStatus(ScoringStatus scoringStatus) { this.scoringStatus = scoringStatus; }
}