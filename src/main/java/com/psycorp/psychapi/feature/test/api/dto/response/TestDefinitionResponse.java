package com.psycorp.psychapi.feature.test.api.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.psycorp.psychapi.feature.test.models.TestDefinition;
import com.psycorp.psychapi.feature.test.models.TestDefinition.*;

public record TestDefinitionResponse(
    String id, String code, String name, String shortName, TestCategory category,
    String description, String instructions, List<TestPurpose> purposes, String version,
    TestStatus status, ScoringMode scoringMode, String scorerKey, Map<String, Object> scoringConfig,
    ResultViewType resultViewType, Integer totalItems, Integer totalDurationSeconds,
    Integer estimatedMinutes, PausePolicy pausePolicy, Integer disconnectGraceSeconds,
    ItemOrderPolicy itemOrderPolicy, TargetAge targetAge, EducationLevel minEducation,
    String language, String requiredFeature, ResultAccessLevel resultAccessLevel,
    Map<String, Object> config, LicenseInfo license, String thumbnailUrl, List<String> tags,
    Instant createdAt, Instant updatedAt
) {
    public static TestDefinitionResponse fromEntity(TestDefinition entity) {
        return new TestDefinitionResponse(
            entity.getId().toHexString(), entity.getCode(), entity.getName(), entity.getShortName(),
            entity.getCategory(), entity.getDescription(), entity.getInstructions(), entity.getPurposes(),
            entity.getVersion(), entity.getStatus(), entity.getScoringMode(), entity.getScorerKey(),
            entity.getScoringConfig(), entity.getResultViewType(), entity.getTotalItems(),
            entity.getTotalDurationSeconds(), entity.getEstimatedMinutes(), entity.getPausePolicy(),
            entity.getDisconnectGraceSeconds(), entity.getItemOrderPolicy(), entity.getTargetAge(),
            entity.getMinEducation(), entity.getLanguage(), entity.getRequiredFeature(),
            entity.getResultAccessLevel(), entity.getConfig(), entity.getLicense(),
            entity.getThumbnailUrl(), entity.getTags(), entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }
}