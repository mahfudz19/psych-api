package com.psycorp.psychapi.feature.test.models;

import java.time.Instant;
import java.util.Map;
import org.bson.types.ObjectId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection = "interpretations")
public class Interpretation extends PanacheMongoEntity {
    private ObjectId resultId;
    private ObjectId attemptId;
    private InterpretationSource source;
    private Integer versionNo = 1;
    private InterpretationStatus status = InterpretationStatus.DRAFT;
    private Boolean isOfficial = false;
    private InterpretationTemplate.Audience audience = InterpretationTemplate.Audience.PARTICIPANT;
    private Map<String, Object> content;
    private ObjectId authorId;
    private ObjectId basedOnId;
    private String templateVersion;
    private Map<String, Object> aiMeta;
    private DisclosureLabel disclosureLabel;
    private ObjectId approvedBy;
    private Instant approvedAt;
    private Instant publishedAt;

    public enum InterpretationSource {
        TEMPLATE("TEMPLATE"), AI("AI"), PSYCHOLOGIST("PSYCHOLOGIST");
        private final String v; InterpretationSource(String v) { this.v = v; }
        @JsonValue public String getValue() { return v; }
        @JsonCreator public static InterpretationSource fromValue(String v) {
            for (InterpretationSource val : values()) if (val.v.equalsIgnoreCase(v)) return val;
            return null;
        }
    }

    public enum InterpretationStatus {
        DRAFT("DRAFT"), IN_REVIEW("IN_REVIEW"), FINAL("FINAL"), PUBLISHED("PUBLISHED");
        private final String v; InterpretationStatus(String v) { this.v = v; }
        @JsonValue public String getValue() { return v; }
        @JsonCreator public static InterpretationStatus fromValue(String v) {
            for (InterpretationStatus val : values()) if (val.v.equalsIgnoreCase(v)) return val;
            return null;
        }
    }

    public enum DisclosureLabel {
        TEMPLATE("TEMPLATE"), AI_GENERATED("AI_GENERATED"), AI_REVIEWED("AI_REVIEWED"), PSYCHOLOGIST("PSYCHOLOGIST");
        private final String v; DisclosureLabel(String v) { this.v = v; }
        @JsonValue public String getValue() { return v; }
        @JsonCreator public static DisclosureLabel fromValue(String v) {
            for (DisclosureLabel val : values()) if (val.v.equalsIgnoreCase(v)) return val;
            return null;
        }
    }

    public ObjectId getResultId() { return resultId; }
    public void setResultId(ObjectId resultId) { this.resultId = resultId; }
    public ObjectId getAttemptId() { return attemptId; }
    public void setAttemptId(ObjectId attemptId) { this.attemptId = attemptId; }
    public InterpretationSource getSource() { return source; }
    public void setSource(InterpretationSource source) { this.source = source; }
    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }
    public InterpretationStatus getStatus() { return status; }
    public void setStatus(InterpretationStatus status) { this.status = status; }
    public Boolean getIsOfficial() { return isOfficial; }
    public void setIsOfficial(Boolean isOfficial) { this.isOfficial = isOfficial; }
    public InterpretationTemplate.Audience getAudience() { return audience; }
    public void setAudience(InterpretationTemplate.Audience audience) { this.audience = audience; }
    public Map<String, Object> getContent() { return content; }
    public void setContent(Map<String, Object> content) { this.content = content; }
    public ObjectId getAuthorId() { return authorId; }
    public void setAuthorId(ObjectId authorId) { this.authorId = authorId; }
    public ObjectId getBasedOnId() { return basedOnId; }
    public void setBasedOnId(ObjectId basedOnId) { this.basedOnId = basedOnId; }
    public String getTemplateVersion() { return templateVersion; }
    public void setTemplateVersion(String templateVersion) { this.templateVersion = templateVersion; }
    public Map<String, Object> getAiMeta() { return aiMeta; }
    public void setAiMeta(Map<String, Object> aiMeta) { this.aiMeta = aiMeta; }
    public DisclosureLabel getDisclosureLabel() { return disclosureLabel; }
    public void setDisclosureLabel(DisclosureLabel disclosureLabel) { this.disclosureLabel = disclosureLabel; }
    public ObjectId getApprovedBy() { return approvedBy; }
    public void setApprovedBy(ObjectId approvedBy) { this.approvedBy = approvedBy; }
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
}