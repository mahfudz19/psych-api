package com.psycorp.psychapi.feature.test.models;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.bson.types.ObjectId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection = "attempt_answers")
public class AttemptAnswer extends PanacheMongoEntity {
    private ObjectId attemptId;
    private ObjectId sectionId;
    private ObjectId testId;
    private List<Map<String, Object>> answers;
    private AnswerStatus status = AnswerStatus.OPEN;
    private Instant savedAt = Instant.now();
    private Integer revision = 0;

    public enum AnswerStatus {
        OPEN("OPEN"), SUBMITTED("SUBMITTED"), AUTO_CLOSED("AUTO_CLOSED");
        private final String v; AnswerStatus(String v) { this.v = v; }
        @JsonValue public String getValue() { return v; }
        @JsonCreator public static AnswerStatus fromValue(String v) {
            for (AnswerStatus val : values()) if (val.v.equalsIgnoreCase(v)) return val;
            return null;
        }
    }

    public ObjectId getAttemptId() { return attemptId; }
    public void setAttemptId(ObjectId attemptId) { this.attemptId = attemptId; }
    public ObjectId getSectionId() { return sectionId; }
    public void setSectionId(ObjectId sectionId) { this.sectionId = sectionId; }
    public ObjectId getTestId() { return testId; }
    public void setTestId(ObjectId testId) { this.testId = testId; }
    public List<Map<String, Object>> getAnswers() { return answers; }
    public void setAnswers(List<Map<String, Object>> answers) { this.answers = answers; }
    public AnswerStatus getStatus() { return status; }
    public void setStatus(AnswerStatus status) { this.status = status; }
    public Instant getSavedAt() { return savedAt; }
    public void setSavedAt(Instant savedAt) { this.savedAt = savedAt; }
    public Integer getRevision() { return revision; }
    public void setRevision(Integer revision) { this.revision = revision; }
}