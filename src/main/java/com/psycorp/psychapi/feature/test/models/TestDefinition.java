package com.psycorp.psychapi.feature.test.models;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection = "test_definitions")
public class TestDefinition extends PanacheMongoEntity {
    private String code;
    private String name;
    private String shortName;
    private TestCategory category;
    private String description;
    private String instructions;
    private List<TestPurpose> purposes;
    private String version;
    private TestStatus status;
    private ScoringMode scoringMode;
    private String scorerKey;
    private Map<String, Object> scoringConfig;
    private ResultViewType resultViewType;
    private Integer totalItems;
    private Integer totalDurationSeconds;
    private Integer estimatedMinutes;
    private PausePolicy pausePolicy;
    private Integer disconnectGraceSeconds;
    private ItemOrderPolicy itemOrderPolicy;
    private TargetAge targetAge;
    private EducationLevel minEducation;
    private String language;
    private String requiredFeature;
    private ResultAccessLevel resultAccessLevel;
    private Map<String, Object> config;
    private LicenseInfo license;
    private String thumbnailUrl;
    private List<String> tags;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    // === CONSTRUCTOR ===
    public TestDefinition() {}

    // === EMBEDDED CLASSES ===
    public static class TargetAge {
        private Integer min;
        private Integer max;

        public TargetAge() {}

        @JsonCreator
        public TargetAge(Integer min, Integer max) {
            this.min = min;
            this.max = max;
        }

        public Integer getMin() { return min; }
        public void setMin(Integer min) { this.min = min; }

        public Integer getMax() { return max; }
        public void setMax(Integer max) { this.max = max; }
    }

    public static class LicenseInfo {
        private String owner;
        private String status;
        private String validUntil;

        public LicenseInfo() {}

        @JsonCreator
        public LicenseInfo(String owner, String status, String validUntil) {
            this.owner = owner;
            this.status = status;
            this.validUntil = validUntil;
        }

        public String getOwner() { return owner; }
        public void setOwner(String owner) { this.owner = owner; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getValidUntil() { return validUntil; }
        public void setValidUntil(String validUntil) { this.validUntil = validUntil; }
    }

    // === ENUMS ===

    public enum TestCategory {
        ABILITY("ABILITY"),
        PERSONALITY_FORCED_CHOICE("PERSONALITY_FORCED_CHOICE"),
        PERSONALITY_SCALE("PERSONALITY_SCALE"),
        INTEREST("INTEREST"),
        SPEED_ENDURANCE("SPEED_ENDURANCE"),
        PROJECTIVE("PROJECTIVE"),
        MANAGEMENT("MANAGEMENT");

        private final String value;
        TestCategory(String value) { this.value = value; }

        @JsonValue
        public String getValue() { return value; }

        @JsonCreator
        public static TestCategory fromValue(String value) {
            if (value == null || value.isBlank()) return null;
            for (TestCategory val : values()) {
                if (val.value.equalsIgnoreCase(value)) return val;
            }
            throw new IllegalArgumentException("Invalid TestCategory: " + value);
        }
    }

    public enum TestPurpose {
        SELF_DISCOVERY("SELF_DISCOVERY"),
        RECRUITMENT("RECRUITMENT");

        private final String value;
        TestPurpose(String value) { this.value = value; }

        @JsonValue
        public String getValue() { return value; }

        @JsonCreator
        public static TestPurpose fromValue(String value) {
            if (value == null || value.isBlank()) return null;
            for (TestPurpose val : values()) {
                if (val.value.equalsIgnoreCase(value)) return val;
            }
            throw new IllegalArgumentException("Invalid TestPurpose: " + value);
        }
    }

    public enum TestStatus {
        DRAFT("DRAFT"),
        ACTIVE("ACTIVE"),
        ARCHIVED("ARCHIVED");

        private final String value;
        TestStatus(String value) { this.value = value; }

        @JsonValue
        public String getValue() { return value; }

        @JsonCreator
        public static TestStatus fromValue(String value) {
            if (value == null || value.isBlank()) return null;
            for (TestStatus val : values()) {
                if (val.value.equalsIgnoreCase(value)) return val;
            }
            throw new IllegalArgumentException("Invalid TestStatus: " + value);
        }
    }

    public enum ScoringMode {
        AUTO("AUTO"),
        MANUAL("MANUAL"),
        HYBRID("HYBRID");

        private final String value;
        ScoringMode(String value) { this.value = value; }

        @JsonValue
        public String getValue() { return value; }

        @JsonCreator
        public static ScoringMode fromValue(String value) {
            if (value == null || value.isBlank()) return null;
            for (ScoringMode val : values()) {
                if (val.value.equalsIgnoreCase(value)) return val;
            }
            throw new IllegalArgumentException("Invalid ScoringMode: " + value);
        }
    }

    public enum ResultViewType {
        RADAR("RADAR"),
        BAR("BAR"),
        PROFILE("PROFILE"),
        TYPE_CARD("TYPE_CARD"),
        IQ_CARD("IQ_CARD"),
        NARRATIVE_ONLY("NARRATIVE_ONLY");

        private final String value;
        ResultViewType(String value) { this.value = value; }

        @JsonValue
        public String getValue() { return value; }

        @JsonCreator
        public static ResultViewType fromValue(String value) {
            if (value == null || value.isBlank()) return null;
            for (ResultViewType val : values()) {
                if (val.value.equalsIgnoreCase(value)) return val;
            }
            throw new IllegalArgumentException("Invalid ResultViewType: " + value);
        }
    }

    public enum PausePolicy {
        FREE("FREE"),
        BETWEEN_SECTIONS_ONLY("BETWEEN_SECTIONS_ONLY"),
        NONE("NONE");

        private final String value;
        PausePolicy(String value) { this.value = value; }

        @JsonValue
        public String getValue() { return value; }

        @JsonCreator
        public static PausePolicy fromValue(String value) {
            if (value == null || value.isBlank()) return null;
            for (PausePolicy val : values()) {
                if (val.value.equalsIgnoreCase(value)) return val;
            }
            throw new IllegalArgumentException("Invalid PausePolicy: " + value);
        }
    }

    public enum ItemOrderPolicy {
        FIXED("FIXED"),
        RANDOMIZED("RANDOMIZED");

        private final String value;
        ItemOrderPolicy(String value) { this.value = value; }

        @JsonValue
        public String getValue() { return value; }

        @JsonCreator
        public static ItemOrderPolicy fromValue(String value) {
            if (value == null || value.isBlank()) return null;
            for (ItemOrderPolicy val : values()) {
                if (val.value.equalsIgnoreCase(value)) return val;
            }
            throw new IllegalArgumentException("Invalid ItemOrderPolicy: " + value);
        }
    }

    public enum EducationLevel {
        SD("SD"),
        SMP("SMP"),
        SMA("SMA"),
        SMK("SMK"),
        D3("D3"),
        S1("S1"),
        S2("S2"),
        S3("S3"),
        OTHER("OTHER");

        private final String value;
        EducationLevel(String value) { this.value = value; }

        @JsonValue
        public String getValue() { return value; }

        @JsonCreator
        public static EducationLevel fromValue(String value) {
            if (value == null || value.isBlank()) return null;
            for (EducationLevel val : values()) {
                if (val.value.equalsIgnoreCase(value)) return val;
            }
            throw new IllegalArgumentException("Invalid EducationLevel: " + value);
        }
    }

    public enum ResultAccessLevel {
        STANDARD("STANDARD"),
        RESTRICTED("RESTRICTED");

        private final String value;
        ResultAccessLevel(String value) { this.value = value; }

        @JsonValue
        public String getValue() { return value; }

        @JsonCreator
        public static ResultAccessLevel fromValue(String value) {
            if (value == null || value.isBlank()) return null;
            for (ResultAccessLevel val : values()) {
                if (val.value.equalsIgnoreCase(value)) return val;
            }
            throw new IllegalArgumentException("Invalid ResultAccessLevel: " + value);
        }
    }

    // === GETTERS ===
    public ObjectId getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getShortName() { return shortName; }
    public TestCategory getCategory() { return category; }
    public String getDescription() { return description; }
    public String getInstructions() { return instructions; }
    public List<TestPurpose> getPurposes() { return purposes; }
    public String getVersion() { return version; }
    public TestStatus getStatus() { return status; }
    public ScoringMode getScoringMode() { return scoringMode; }
    public String getScorerKey() { return scorerKey; }
    public Map<String, Object> getScoringConfig() { return scoringConfig; }
    public ResultViewType getResultViewType() { return resultViewType; }
    public Integer getTotalItems() { return totalItems; }
    public Integer getTotalDurationSeconds() { return totalDurationSeconds; }
    public Integer getEstimatedMinutes() { return estimatedMinutes; }
    public PausePolicy getPausePolicy() { return pausePolicy; }
    public Integer getDisconnectGraceSeconds() { return disconnectGraceSeconds; }
    public ItemOrderPolicy getItemOrderPolicy() { return itemOrderPolicy; }
    public TargetAge getTargetAge() { return targetAge; }
    public EducationLevel getMinEducation() { return minEducation; }
    public String getLanguage() { return language; }
    public String getRequiredFeature() { return requiredFeature; }
    public ResultAccessLevel getResultAccessLevel() { return resultAccessLevel; }
    public Map<String, Object> getConfig() { return config; }
    public LicenseInfo getLicense() { return license; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public List<String> getTags() { return tags; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }

    // === SETTERS ===
    public void setCode(String code) { this.code = code; }
    public void setName(String name) { this.name = name; }
    public void setShortName(String shortName) { this.shortName = shortName; }
    public void setCategory(TestCategory category) { this.category = category; }
    public void setDescription(String description) { this.description = description; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public void setPurposes(List<TestPurpose> purposes) { this.purposes = purposes; }
    public void setVersion(String version) { this.version = version; }
    public void setStatus(TestStatus status) { this.status = status; }
    public void setScoringMode(ScoringMode scoringMode) { this.scoringMode = scoringMode; }
    public void setScorerKey(String scorerKey) { this.scorerKey = scorerKey; }
    public void setScoringConfig(Map<String, Object> scoringConfig) { this.scoringConfig = scoringConfig; }
    public void setResultViewType(ResultViewType resultViewType) { this.resultViewType = resultViewType; }
    public void setTotalItems(Integer totalItems) { this.totalItems = totalItems; }
    public void setTotalDurationSeconds(Integer totalDurationSeconds) { this.totalDurationSeconds = totalDurationSeconds; }
    public void setEstimatedMinutes(Integer estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }
    public void setPausePolicy(PausePolicy pausePolicy) { this.pausePolicy = pausePolicy; }
    public void setDisconnectGraceSeconds(Integer disconnectGraceSeconds) { this.disconnectGraceSeconds = disconnectGraceSeconds; }
    public void setItemOrderPolicy(ItemOrderPolicy itemOrderPolicy) { this.itemOrderPolicy = itemOrderPolicy; }
    public void setTargetAge(TargetAge targetAge) { this.targetAge = targetAge; }
    public void setMinEducation(EducationLevel minEducation) { this.minEducation = minEducation; }
    public void setLanguage(String language) { this.language = language; }
    public void setRequiredFeature(String requiredFeature) { this.requiredFeature = requiredFeature; }
    public void setResultAccessLevel(ResultAccessLevel resultAccessLevel) { this.resultAccessLevel = resultAccessLevel; }
    public void setConfig(Map<String, Object> config) { this.config = config; }
    public void setLicense(LicenseInfo license) { this.license = license; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }

}