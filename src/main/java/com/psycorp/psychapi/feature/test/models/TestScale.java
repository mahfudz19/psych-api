package com.psycorp.psychapi.feature.test.models;

import java.time.Instant;
import java.util.Map;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection = "test_scales")
public class TestScale extends PanacheMongoEntity {
    private ObjectId testId;
    private String code;
    private String name;
    private ScaleType type = ScaleType.DIMENSION;
    private ScaleRole role = ScaleRole.PERFORMANCE;
    private ObjectId parentScaleId;
    private ObjectId sectionId;
    private Integer order = 0;
    private String description;
    private String lowLabel;
    private String highLabel;
    private Integer rawMin;
    private Integer rawMax;
    private ScoreType scoreType = ScoreType.RAW_ONLY;
    private Map<String, Object> composite;
    private Boolean isReported = true;
    private Boolean isValidityScale = false;
    private String color;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    public enum ScaleType {
        DIMENSION("DIMENSION"), FACET("FACET"), SUBTEST("SUBTEST"), VALIDITY("VALIDITY"), COMPOSITE("COMPOSITE"), TYPE_POLE("TYPE_POLE");
        private final String v; ScaleType(String v) { this.v = v; }
        @JsonValue public String getValue() { return v; }
        @JsonCreator public static ScaleType fromValue(String v) {
            for (ScaleType val : values()) if (val.v.equalsIgnoreCase(v)) return val;
            throw new IllegalArgumentException("Invalid ScaleType: " + v);
        }
    }

    public enum ScaleRole {
        PERFORMANCE("PERFORMANCE"), EXPECTATION("EXPECTATION"), IMPORTANCE("IMPORTANCE"), REFLECTION("REFLECTION");
        private final String v; ScaleRole(String v) { this.v = v; }
        @JsonValue public String getValue() { return v; }
        @JsonCreator public static ScaleRole fromValue(String v) {
            for (ScaleRole val : values()) if (val.v.equalsIgnoreCase(v)) return val;
            throw new IllegalArgumentException("Invalid ScaleRole: " + v);
        }
    }

    public enum ScoreType {
        RAW_ONLY("RAW_ONLY"), RAW_AVERAGE("RAW_AVERAGE"), RAW_TO_SW("RAW_TO_SW"), RAW_TO_WS("RAW_TO_WS"),
        RAW_TO_IQ("RAW_TO_IQ"), RAW_TO_T("RAW_TO_T"), RAW_TO_STEN("RAW_TO_STEN"), RAW_TO_PERCENTILE("RAW_TO_PERCENTILE"),
        IPSATIVE("IPSATIVE"), RANK_SUM("RANK_SUM");
        private final String v; ScoreType(String v) { this.v = v; }
        @JsonValue public String getValue() { return v; }
        @JsonCreator public static ScoreType fromValue(String v) {
            for (ScoreType val : values()) if (val.v.equalsIgnoreCase(v)) return val;
            throw new IllegalArgumentException("Invalid ScoreType: " + v);
        }
    }

    public ObjectId getId() { return id; }
    public ObjectId getTestId() { return testId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public ScaleType getType() { return type; }
    public ScaleRole getRole() { return role; }
    public ObjectId getParentScaleId() { return parentScaleId; }
    public ObjectId getSectionId() { return sectionId; }
    public Integer getOrder() { return order; }
    public String getDescription() { return description; }
    public String getLowLabel() { return lowLabel; }
    public String getHighLabel() { return highLabel; }
    public Integer getRawMin() { return rawMin; }
    public Integer getRawMax() { return rawMax; }
    public ScoreType getScoreType() { return scoreType; }
    public Map<String, Object> getComposite() { return composite; }
    public Boolean getIsReported() { return isReported; }
    public Boolean getIsValidityScale() { return isValidityScale; }
    public String getColor() { return color; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }

    public void setTestId(ObjectId testId) { this.testId = testId; }
    public void setCode(String code) { this.code = code; }
    public void setName(String name) { this.name = name; }
    public void setType(ScaleType type) { this.type = type; }
    public void setRole(ScaleRole role) { this.role = role; }
    public void setParentScaleId(ObjectId parentScaleId) { this.parentScaleId = parentScaleId; }
    public void setSectionId(ObjectId sectionId) { this.sectionId = sectionId; }
    public void setOrder(Integer order) { this.order = order; }
    public void setDescription(String description) { this.description = description; }
    public void setLowLabel(String lowLabel) { this.lowLabel = lowLabel; }
    public void setHighLabel(String highLabel) { this.highLabel = highLabel; }
    public void setRawMin(Integer rawMin) { this.rawMin = rawMin; }
    public void setRawMax(Integer rawMax) { this.rawMax = rawMax; }
    public void setScoreType(ScoreType scoreType) { this.scoreType = scoreType; }
    public void setComposite(Map<String, Object> composite) { this.composite = composite; }
    public void setIsReported(Boolean isReported) { this.isReported = isReported; }
    public void setIsValidityScale(Boolean isValidityScale) { this.isValidityScale = isValidityScale; }
    public void setColor(String color) { this.color = color; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}