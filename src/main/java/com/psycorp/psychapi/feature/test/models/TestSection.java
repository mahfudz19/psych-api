// 1. TestSection.java
package com.psycorp.psychapi.feature.test.models;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection = "test_sections")
public class TestSection extends PanacheMongoEntity {
    private ObjectId testId;
    private String testVersion;
    private String code;
    private Integer order;
    private String name;
    private String instructions;
    private List<Map<String, Object>> examples;
    private Boolean showExamplesFirst = true;
    private Integer timeLimitSeconds;
    private Integer itemCount = 0;
    private NavigationPolicy navigationPolicy = NavigationPolicy.FREE;
    private DefaultItemType defaultItemType = DefaultItemType.SINGLE_CHOICE;
    private List<ObjectId> scaleIds;
    private Map<String, Object> stimulusPhase;
    private Map<String, Object> config;
    private SectionStatus status = SectionStatus.ACTIVE;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    public enum NavigationPolicy {
        FREE("FREE"), FORWARD_ONLY("FORWARD_ONLY");
        private final String v; NavigationPolicy(String v) { this.v = v; }
        @JsonValue public String getValue() { return v; }
        @JsonCreator public static NavigationPolicy fromValue(String v) {
            for (NavigationPolicy val : values()) if (val.v.equalsIgnoreCase(v)) return val;
            throw new IllegalArgumentException("Invalid NavigationPolicy: " + v);
        }
    }

    public enum DefaultItemType {
        SINGLE_CHOICE("SINGLE_CHOICE"), MULTI_CHOICE("MULTI_CHOICE"), TRUE_FALSE("TRUE_FALSE"),
        YES_NO("YES_NO"), LIKERT("LIKERT"), FORCED_CHOICE("FORCED_CHOICE"), MOST_LEAST("MOST_LEAST"),
        RANKING("RANKING"), NUMERIC_INPUT("NUMERIC_INPUT"), TEXT_INPUT("TEXT_INPUT"), DRAWING("DRAWING");
        private final String v; DefaultItemType(String v) { this.v = v; }
        @JsonValue public String getValue() { return v; }
        @JsonCreator public static DefaultItemType fromValue(String v) {
            for (DefaultItemType val : values()) if (val.v.equalsIgnoreCase(v)) return val;
            throw new IllegalArgumentException("Invalid DefaultItemType: " + v);
        }
    }

    public enum SectionStatus {
        ACTIVE("ACTIVE"), RETIRED("RETIRED");
        private final String v; SectionStatus(String v) { this.v = v; }
        @JsonValue public String getValue() { return v; }
        @JsonCreator public static SectionStatus fromValue(String v) {
            for (SectionStatus val : values()) if (val.v.equalsIgnoreCase(v)) return val;
            throw new IllegalArgumentException("Invalid SectionStatus: " + v);
        }
    }

    // Getters & Setters
    public ObjectId getId() { return id; }
    public ObjectId getTestId() { return testId; }
    public String getTestVersion() { return testVersion; }
    public String getCode() { return code; }
    public Integer getOrder() { return order; }
    public String getName() { return name; }
    public String getInstructions() { return instructions; }
    public List<Map<String, Object>> getExamples() { return examples; }
    public Boolean getShowExamplesFirst() { return showExamplesFirst; }
    public Integer getTimeLimitSeconds() { return timeLimitSeconds; }
    public Integer getItemCount() { return itemCount; }
    public NavigationPolicy getNavigationPolicy() { return navigationPolicy; }
    public DefaultItemType getDefaultItemType() { return defaultItemType; }
    public List<ObjectId> getScaleIds() { return scaleIds; }
    public Map<String, Object> getStimulusPhase() { return stimulusPhase; }
    public Map<String, Object> getConfig() { return config; }
    public SectionStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }


    public void setTestId(ObjectId testId) { this.testId = testId; }
    public void setTestVersion(String testVersion) { this.testVersion = testVersion; }
    public void setCode(String code) { this.code = code; }
    public void setOrder(Integer order) { this.order = order; }
    public void setName(String name) { this.name = name; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public void setExamples(List<Map<String, Object>> examples) { this.examples = examples; }
    public void setShowExamplesFirst(Boolean showExamplesFirst) { this.showExamplesFirst = showExamplesFirst; }
    public void setTimeLimitSeconds(Integer timeLimitSeconds) { this.timeLimitSeconds = timeLimitSeconds; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
    public void setNavigationPolicy(NavigationPolicy navigationPolicy) { this.navigationPolicy = navigationPolicy; }
    public void setDefaultItemType(DefaultItemType defaultItemType) { this.defaultItemType = defaultItemType; }
    public void setScaleIds(List<ObjectId> scaleIds) { this.scaleIds = scaleIds; }
    public void setStimulusPhase(Map<String, Object> stimulusPhase) { this.stimulusPhase = stimulusPhase; }
    public void setConfig(Map<String, Object> config) { this.config = config; }
    public void setStatus(SectionStatus status) { this.status = status; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}