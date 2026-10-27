package com.psycorp.psychapi.feature.test.models;

import java.time.Instant;
import java.util.Map;
import org.bson.types.ObjectId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection = "test_assignments")
public class TestAssignment extends PanacheMongoEntity {
    private ObjectId workspaceId;
    private ObjectId participantId;
    private ObjectId userId;
    private ObjectId assignedBy;
    private ObjectId testId;
    private String testVersion;
    private ObjectId packageId;
    private ObjectId subscriptionId;
    private Map<String, Object> policyProfile;
    private TestDefinition.TestPurpose purpose = TestDefinition.TestPurpose.SELF_DISCOVERY;
    private String accessToken;
    private Instant availableFrom = Instant.now();
    private Instant availableUntil;
    private Integer maxAttempts = 1;
    private Map<String, Object> participantSnapshot;
    private ObjectId consentId;
    private AssignmentStatus status = AssignmentStatus.CREATED;

    public enum AssignmentStatus {
        CREATED("CREATED"), STARTED("STARTED"), COMPLETED("COMPLETED"), EXPIRED("EXPIRED"), CANCELLED("CANCELLED");
        private final String v; AssignmentStatus(String v) { this.v = v; }
        @JsonValue public String getValue() { return v; }
        @JsonCreator public static AssignmentStatus fromValue(String v) {
            for (AssignmentStatus val : values()) if (val.v.equalsIgnoreCase(v)) return val;
            return null;
        }
    }

    public ObjectId getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(ObjectId workspaceId) { this.workspaceId = workspaceId; }
    public ObjectId getParticipantId() { return participantId; }
    public void setParticipantId(ObjectId participantId) { this.participantId = participantId; }
    public ObjectId getUserId() { return userId; }
    public void setUserId(ObjectId userId) { this.userId = userId; }
    public ObjectId getAssignedBy() { return assignedBy; }
    public void setAssignedBy(ObjectId assignedBy) { this.assignedBy = assignedBy; }
    public ObjectId getTestId() { return testId; }
    public void setTestId(ObjectId testId) { this.testId = testId; }
    public String getTestVersion() { return testVersion; }
    public void setTestVersion(String testVersion) { this.testVersion = testVersion; }
    public ObjectId getPackageId() { return packageId; }
    public void setPackageId(ObjectId packageId) { this.packageId = packageId; }
    public ObjectId getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(ObjectId subscriptionId) { this.subscriptionId = subscriptionId; }
    public Map<String, Object> getPolicyProfile() { return policyProfile; }
    public void setPolicyProfile(Map<String, Object> policyProfile) { this.policyProfile = policyProfile; }
    public TestDefinition.TestPurpose getPurpose() { return purpose; }
    public void setPurpose(TestDefinition.TestPurpose purpose) { this.purpose = purpose; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public Instant getAvailableFrom() { return availableFrom; }
    public void setAvailableFrom(Instant availableFrom) { this.availableFrom = availableFrom; }
    public Instant getAvailableUntil() { return availableUntil; }
    public void setAvailableUntil(Instant availableUntil) { this.availableUntil = availableUntil; }
    public Integer getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(Integer maxAttempts) { this.maxAttempts = maxAttempts; }
    public Map<String, Object> getParticipantSnapshot() { return participantSnapshot; }
    public void setParticipantSnapshot(Map<String, Object> participantSnapshot) { this.participantSnapshot = participantSnapshot; }
    public ObjectId getConsentId() { return consentId; }
    public void setConsentId(ObjectId consentId) { this.consentId = consentId; }
    public AssignmentStatus getStatus() { return status; }
    public void setStatus(AssignmentStatus status) { this.status = status; }
}