package com.psycorp.psychapi.feature.test.models;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.bson.types.ObjectId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection = "test_items")
public class TestItem extends PanacheMongoEntity {
    private ObjectId testId;
    private ObjectId sectionId;
    private String testVersion;
    private Integer number;
    private ItemType type;
    private Map<String, Object> content;
    private List<Map<String, Object>> options;
    private String groupId;
    private ObjectId scaleId;
    private Boolean reverseScored = false;
    private Double weight = 1.0;
    private Boolean scoredInTotal = true;
    private String consistencyGroup;
    private Map<String, Object> answerKey;
    private Integer timeLimitSeconds;
    private Map<String, Object> constraints;
    private Double difficulty;
    private Boolean isExample = false;
    private ItemStatus status = ItemStatus.ACTIVE;
    private List<String> tags;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    public enum ItemType {
        SINGLE_CHOICE("SINGLE_CHOICE"), MULTI_CHOICE("MULTI_CHOICE"), TRUE_FALSE("TRUE_FALSE"), YES_NO("YES_NO"),
        LIKERT("LIKERT"), FORCED_CHOICE("FORCED_CHOICE"), MOST_LEAST("MOST_LEAST"), RANKING("RANKING"),
        NUMERIC_INPUT("NUMERIC_INPUT"), TEXT_INPUT("TEXT_INPUT"), DRAWING("DRAWING"), MATRIX_TASK("MATRIX_TASK");

        private final String v;

        ItemType(String v) {
            this.v = v;
        }

        @JsonValue
        public String getValue() {
            return v;
        }

        @JsonCreator
        public static ItemType fromValue(String v) {
            for (ItemType val : values())
                if (val.v.equalsIgnoreCase(v))
                    return val;
            return null;
        }
    }

    public enum ItemStatus {
        ACTIVE("ACTIVE"), RETIRED("RETIRED");

        private final String v;

        ItemStatus(String v) {
            this.v = v;
        }

        @JsonValue
        public String getValue() {
            return v;
        }

        @JsonCreator
        public static ItemStatus fromValue(String v) {
            for (ItemStatus val : values())
                if (val.v.equalsIgnoreCase(v))
                    return val;
            return null;
        }
    }

    public ObjectId getTestId() {
        return testId;
    }

    public void setTestId(ObjectId testId) {
        this.testId = testId;
    }

    public ObjectId getSectionId() {
        return sectionId;
    }

    public void setSectionId(ObjectId sectionId) {
        this.sectionId = sectionId;
    }

    public String getTestVersion() {
        return testVersion;
    }

    public void setTestVersion(String testVersion) {
        this.testVersion = testVersion;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public ItemType getType() {
        return type;
    }

    public void setType(ItemType type) {
        this.type = type;
    }

    public Map<String, Object> getContent() {
        return content;
    }

    public void setContent(Map<String, Object> content) {
        this.content = content;
    }

    public List<Map<String, Object>> getOptions() {
        return options;
    }

    public void setOptions(List<Map<String, Object>> options) {
        this.options = options;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public ObjectId getScaleId() {
        return scaleId;
    }

    public void setScaleId(ObjectId scaleId) {
        this.scaleId = scaleId;
    }

    public Boolean getReverseScored() {
        return reverseScored;
    }

    public void setReverseScored(Boolean reverseScored) {
        this.reverseScored = reverseScored;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Boolean getScoredInTotal() {
        return scoredInTotal;
    }

    public void setScoredInTotal(Boolean scoredInTotal) {
        this.scoredInTotal = scoredInTotal;
    }

    public String getConsistencyGroup() {
        return consistencyGroup;
    }

    public void setConsistencyGroup(String consistencyGroup) {
        this.consistencyGroup = consistencyGroup;
    }

    public Map<String, Object> getAnswerKey() {
        return answerKey;
    }

    public void setAnswerKey(Map<String, Object> answerKey) {
        this.answerKey = answerKey;
    }

    public Integer getTimeLimitSeconds() {
        return timeLimitSeconds;
    }

    public void setTimeLimitSeconds(Integer timeLimitSeconds) {
        this.timeLimitSeconds = timeLimitSeconds;
    }

    public Map<String, Object> getConstraints() {
        return constraints;
    }

    public void setConstraints(Map<String, Object> constraints) {
        this.constraints = constraints;
    }

    public Double getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Double difficulty) {
        this.difficulty = difficulty;
    }

    public Boolean getIsExample() {
        return isExample;
    }

    public void setIsExample(Boolean isExample) {
        this.isExample = isExample;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public ObjectId getId() {
        return id;
    }
}