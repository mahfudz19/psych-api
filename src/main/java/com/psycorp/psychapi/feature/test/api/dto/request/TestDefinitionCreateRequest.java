package com.psycorp.psychapi.feature.test.api.dto.request;

import java.util.List;
import java.util.Map;

import com.psycorp.psychapi.feature.test.models.TestDefinition.EducationLevel;
import com.psycorp.psychapi.feature.test.models.TestDefinition.ItemOrderPolicy;
import com.psycorp.psychapi.feature.test.models.TestDefinition.LicenseInfo;
import com.psycorp.psychapi.feature.test.models.TestDefinition.PausePolicy;
import com.psycorp.psychapi.feature.test.models.TestDefinition.ResultAccessLevel;
import com.psycorp.psychapi.feature.test.models.TestDefinition.ResultViewType;
import com.psycorp.psychapi.feature.test.models.TestDefinition.ScoringMode;
import com.psycorp.psychapi.feature.test.models.TestDefinition.TargetAge;
import com.psycorp.psychapi.feature.test.models.TestDefinition.TestCategory;
import com.psycorp.psychapi.feature.test.models.TestDefinition.TestPurpose;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TestDefinitionCreateRequest(
    @NotBlank(message = "Code is required") String code,
    @NotBlank(message = "Name is required") String name,
    @NotBlank(message = "Short name is required") String shortName,
    @NotNull(message = "Category is required") TestCategory category,
    String description,
    String instructions,
    List<TestPurpose> purposes,
    String version,
    ScoringMode scoringMode,
    String scorerKey,
    Map<String, Object> scoringConfig,
    ResultViewType resultViewType,
    Integer totalItems,
    Integer totalDurationSeconds,
    Integer estimatedMinutes,
    PausePolicy pausePolicy,
    Integer disconnectGraceSeconds,
    ItemOrderPolicy itemOrderPolicy,
    TargetAge targetAge,
    EducationLevel minEducation,
    String language,
    String requiredFeature,
    ResultAccessLevel resultAccessLevel,
    Map<String, Object> config,
    LicenseInfo license,
    String thumbnailUrl,
    List<String> tags
) {}