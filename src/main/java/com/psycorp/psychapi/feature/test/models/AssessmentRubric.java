package com.psycorp.psychapi.feature.test.models;

import java.util.List;
import java.util.Map;
import org.bson.types.ObjectId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection = "assessment_rubrics")
public class AssessmentRubric extends PanacheMongoEntity {
    private ObjectId testId;
    private String code;
    private String version = "1.0";
    private List<Map<String, Object>> aspects;
    private Boolean perItem = false;
    private RubricStatus status = RubricStatus.DRAFT;

    public enum RubricStatus {
        DRAFT("DRAFT"), ACTIVE("ACTIVE"), ARCHIVED("ARCHIVED");
        private final String v; RubricStatus(String v) { this.v = v; }
        @JsonValue public String getValue() { return v; }
        @JsonCreator public static RubricStatus fromValue(String v) {
            for (RubricStatus val : values()) if (val.v.equalsIgnoreCase(v)) return val;
            return null;
        }
    }

    public ObjectId getTestId() { return testId; }
    public void setTestId(ObjectId testId) { this.testId = testId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public List<Map<String, Object>> getAspects() { return aspects; }
    public void setAspects(List<Map<String, Object>> aspects) { this.aspects = aspects; }
    public Boolean getPerItem() { return perItem; }
    public void setPerItem(Boolean perItem) { this.perItem = perItem; }
    public RubricStatus getStatus() { return status; }
    public void setStatus(RubricStatus status) { this.status = status; }
}