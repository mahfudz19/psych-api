package com.psycorp.psychapi.feature.test.models;

import java.util.List;
import java.util.Map;
import org.bson.types.ObjectId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection = "interpretation_templates")
public class InterpretationTemplate extends PanacheMongoEntity {
    private ObjectId testId;
    private String testVersion;
    private TemplateScope scope;
    private ObjectId scaleId;
    private String levelKey;
    private String typeKey;
    private Map<String, Object> scoreRange;
    private String language = "id";
    private Audience audience = Audience.PARTICIPANT;
    private String title;
    private String summary = "";
    private String narrative = "";
    private List<String> strengths;
    private List<String> developmentAreas;
    private List<String> recommendations;
    private List<String> careerSuggestions;
    private String requiredFeature;
    private String version = "1.0";
    private TemplateStatus status = TemplateStatus.DRAFT;

    public enum TemplateScope {
        SCALE_LEVEL("SCALE_LEVEL"), TYPE("TYPE"), COMBINATION("COMBINATION"), GAP("GAP"), OVERALL("OVERALL");
        private final String v; TemplateScope(String v) { this.v = v; }
        @JsonValue public String getValue() { return v; }
        @JsonCreator public static TemplateScope fromValue(String v) {
            for (TemplateScope val : values()) if (val.v.equalsIgnoreCase(v)) return val;
            return null;
        }
    }

    public enum Audience {
        PARTICIPANT("PARTICIPANT"), ORGANIZATION("ORGANIZATION"), PSYCHOLOGIST("PSYCHOLOGIST");
        private final String v; Audience(String v) { this.v = v; }
        @JsonValue public String getValue() { return v; }
        @JsonCreator public static Audience fromValue(String v) {
            for (Audience val : values()) if (val.v.equalsIgnoreCase(v)) return val;
            return null;
        }
    }

    public enum TemplateStatus {
        DRAFT("DRAFT"), ACTIVE("ACTIVE"), ARCHIVED("ARCHIVED");
        private final String v; TemplateStatus(String v) { this.v = v; }
        @JsonValue public String getValue() { return v; }
        @JsonCreator public static TemplateStatus fromValue(String v) {
            for (TemplateStatus val : values()) if (val.v.equalsIgnoreCase(v)) return val;
            return null;
        }
    }

    public ObjectId getTestId() { return testId; }
    public void setTestId(ObjectId testId) { this.testId = testId; }
    public String getTestVersion() { return testVersion; }
    public void setTestVersion(String testVersion) { this.testVersion = testVersion; }
    public TemplateScope getScope() { return scope; }
    public void setScope(TemplateScope scope) { this.scope = scope; }
    public ObjectId getScaleId() { return scaleId; }
    public void setScaleId(ObjectId scaleId) { this.scaleId = scaleId; }
    public String getLevelKey() { return levelKey; }
    public void setLevelKey(String levelKey) { this.levelKey = levelKey; }
    public String getTypeKey() { return typeKey; }
    public void setTypeKey(String typeKey) { this.typeKey = typeKey; }
    public Map<String, Object> getScoreRange() { return scoreRange; }
    public void setScoreRange(Map<String, Object> scoreRange) { this.scoreRange = scoreRange; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public Audience getAudience() { return audience; }
    public void setAudience(Audience audience) { this.audience = audience; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getNarrative() { return narrative; }
    public void setNarrative(String narrative) { this.narrative = narrative; }
    public List<String> getStrengths() { return strengths; }
    public void setStrengths(List<String> strengths) { this.strengths = strengths; }
    public List<String> getDevelopmentAreas() { return developmentAreas; }
    public void setDevelopmentAreas(List<String> developmentAreas) { this.developmentAreas = developmentAreas; }
    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    public List<String> getCareerSuggestions() { return careerSuggestions; }
    public void setCareerSuggestions(List<String> careerSuggestions) { this.careerSuggestions = careerSuggestions; }
    public String getRequiredFeature() { return requiredFeature; }
    public void setRequiredFeature(String requiredFeature) { this.requiredFeature = requiredFeature; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public TemplateStatus getStatus() { return status; }
    public void setStatus(TemplateStatus status) { this.status = status; }
}