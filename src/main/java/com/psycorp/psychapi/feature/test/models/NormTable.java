package com.psycorp.psychapi.feature.test.models;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.bson.types.ObjectId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection = "norm_tables")
public class NormTable extends PanacheMongoEntity {
    private ObjectId testId;
    private String testVersion;
    private String normVersion;
    private String name;
    private ObjectId scaleId;
    private ObjectId sectionId;
    private String targetScoreType;
    private NormMethod method = NormMethod.LOOKUP;
    private Map<String, Object> criteria;
    private List<Map<String, Object>> entries;
    private Double mean;
    private Double sd;
    private List<Map<String, Object>> categoryCutoffs;
    private Integer sampleSize;
    private String source;
    private Instant validFrom = Instant.now();
    private Instant validUntil;
    private NormStatus status = NormStatus.DRAFT;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    public enum NormMethod {
        LOOKUP("LOOKUP"), MEAN_SD("MEAN_SD");
        private final String v; NormMethod(String v) { this.v = v; }
        @JsonValue public String getValue() { return v; }
        @JsonCreator public static NormMethod fromValue(String v) {
            for (NormMethod val : values()) if (val.v.equalsIgnoreCase(v)) return val;
            return null;
        }
    }

    public enum NormStatus {
        DRAFT("DRAFT"), ACTIVE("ACTIVE"), ARCHIVED("ARCHIVED");
        private final String v; NormStatus(String v) { this.v = v; }
        @JsonValue public String getValue() { return v; }
        @JsonCreator public static NormStatus fromValue(String v) {
            for (NormStatus val : values()) if (val.v.equalsIgnoreCase(v)) return val;
            return null;
        }
    }

    public ObjectId getTestId() { return testId; }
    public void setTestId(ObjectId testId) { this.testId = testId; }
    public String getTestVersion() { return testVersion; }
    public void setTestVersion(String testVersion) { this.testVersion = testVersion; }
    public String getNormVersion() { return normVersion; }
    public void setNormVersion(String normVersion) { this.normVersion = normVersion; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ObjectId getScaleId() { return scaleId; }
    public void setScaleId(ObjectId scaleId) { this.scaleId = scaleId; }
    public ObjectId getSectionId() { return sectionId; }
    public void setSectionId(ObjectId sectionId) { this.sectionId = sectionId; }
    public String getTargetScoreType() { return targetScoreType; }
    public void setTargetScoreType(String targetScoreType) { this.targetScoreType = targetScoreType; }
    public NormMethod getMethod() { return method; }
    public void setMethod(NormMethod method) { this.method = method; }
    public Map<String, Object> getCriteria() { return criteria; }
    public void setCriteria(Map<String, Object> criteria) { this.criteria = criteria; }
    public List<Map<String, Object>> getEntries() { return entries; }
    public void setEntries(List<Map<String, Object>> entries) { this.entries = entries; }
    public Double getMean() { return mean; }
    public void setMean(Double mean) { this.mean = mean; }
    public Double getSd() { return sd; }
    public void setSd(Double sd) { this.sd = sd; }
    public List<Map<String, Object>> getCategoryCutoffs() { return categoryCutoffs; }
    public void setCategoryCutoffs(List<Map<String, Object>> categoryCutoffs) { this.categoryCutoffs = categoryCutoffs; }
    public Integer getSampleSize() { return sampleSize; }
    public void setSampleSize(Integer sampleSize) { this.sampleSize = sampleSize; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Instant getValidFrom() { return validFrom; }
    public void setValidFrom(Instant validFrom) { this.validFrom = validFrom; }
    public Instant getValidUntil() { return validUntil; }
    public void setValidUntil(Instant validUntil) { this.validUntil = validUntil; }
    public NormStatus getStatus() { return status; }
    public void setStatus(NormStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}