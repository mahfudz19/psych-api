package com.psycorp.psychapi.feature.test.api.dto.request;

import java.util.Map;

import org.bson.types.ObjectId;

import com.psycorp.psychapi.feature.test.models.TestScale.ScaleRole;
import com.psycorp.psychapi.feature.test.models.TestScale.ScaleType;
import com.psycorp.psychapi.feature.test.models.TestScale.ScoreType;

import jakarta.validation.constraints.NotBlank;

public record TestScaleCreateRequest(
    @NotBlank(message = "Code is required") String code,
    @NotBlank(message = "Name is required") String name,
    ScaleType type,
    ScaleRole role,
    ObjectId parentScaleId,
    ObjectId sectionId,
    Integer order,
    String description,
    String lowLabel,
    String highLabel,
    Integer rawMin,
    Integer rawMax,
    ScoreType scoreType,
    Map<String, Object> composite,
    Boolean isReported,
    Boolean isValidityScale,
    String color
) {}