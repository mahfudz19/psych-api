package com.psycorp.psychapi.feature.test.models;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.bson.types.ObjectId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection = "test_results")
public class TestResult extends PanacheMongoEntity {
    private ObjectId attemptId;
    private ObjectId workspaceId;
    private ObjectId participantId;
    private ObjectId testId;
    private String testVersion;
    private String scorerVersion;
    private String normVersion;
    private ResultStatus status = ResultStatus.PENDING;
    private List<Map<String, Object>> scaleScores;
    private Map<String, Object> derived;
    private Map<String, Object> validity;
    private List<Map<String, Object>> gapAnalysis;
    private Map<String, Object> manualAssessment;
    private Map<String, Object> conditions;
    private Boolean requiresReview = false;
    private Instant calculatedAt = Instant.now();

    public enum ResultStatus {
        PENDING("PENDING"), CALCULATED("CALCULATED"), AWAITING_MANUAL("AWAITING_MANUAL"), FINAL("FINAL");
        private final String v; ResultStatus(String v) { this.v = v; }
        @JsonValue public String getValue() { return v; }
        @JsonCreator public static ResultStatus fromValue(String v) {
            for (ResultStatus val : values()) if (val.v.equalsIgnoreCase(v)) return val;
            return null;
        }
    }

    public ObjectId getAttemptId() { return attemptId; }
    public void setAttemptId(ObjectId attemptId) { this.attemptId = attemptId; }
    public ObjectId getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(ObjectId workspaceId) { this.workspaceId = workspaceId; }
    public ObjectId getParticipantId() { return participantId; }
    public void setParticipantId(ObjectId participantId) { this.participantId = participantId; }
    public ObjectId getTestId() { return testId; }
    public void setTestId(ObjectId testId) { this.testId = testId; }
    public String getTestVersion() { return testVersion; }
    public void setTestVersion(String testVersion) { this.testVersion = testVersion; }
    public String getScorerVersion() { return scorerVersion; }
    public void setScorerVersion(String scorerVersion) { this.scorerVersion = scorerVersion; }
    public String getNormVersion() { return normVersion; }
    public void setNormVersion(String normVersion) { this.normVersion = normVersion; }
    public ResultStatus getStatus() { return status; }
    public void setStatus(ResultStatus status) { this.status = status; }
    public List<Map<String, Object>> getScaleScores() { return scaleScores; }
    public void setScaleScores(List<Map<String, Object>> scaleScores) { this.scaleScores = scaleScores; }
    public Map<String, Object> getDerived() { return derived; }
    public void setDerived(Map<String, Object> derived) { this.derived = derived; }
    public Map<String, Object> getValidity() { return validity; }
    public void setValidity(Map<String, Object> validity) { this.validity = validity; }
    public List<Map<String, Object>> getGapAnalysis() { return gapAnalysis; }
    public void setGapAnalysis(List<Map<String, Object>> gapAnalysis) { this.gapAnalysis = gapAnalysis; }
    public Map<String, Object> getManualAssessment() { return manualAssessment; }
    public void setManualAssessment(Map<String, Object> manualAssessment) { this.manualAssessment = manualAssessment; }
    public Map<String, Object> getConditions() { return conditions; }
    public void setConditions(Map<String, Object> conditions) { this.conditions = conditions; }
    public Boolean getRequiresReview() { return requiresReview; }
    public void setRequiresReview(Boolean requiresReview) { this.requiresReview = requiresReview; }
    public Instant getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(Instant calculatedAt) { this.calculatedAt = calculatedAt; }
}