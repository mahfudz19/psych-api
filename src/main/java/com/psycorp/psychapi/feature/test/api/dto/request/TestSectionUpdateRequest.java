package com.psycorp.psychapi.feature.test.api.dto.request;

import java.util.List;
import java.util.Map;

import com.psycorp.psychapi.feature.test.models.TestSection.DefaultItemType;
import com.psycorp.psychapi.feature.test.models.TestSection.NavigationPolicy;
import com.psycorp.psychapi.feature.test.models.TestSection.SectionStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TestSectionUpdateRequest(
    @NotNull(message = "Order is required") Integer order,
    @NotBlank(message = "Name is required") String name,
    String instructions,
    List<Map<String, Object>> examples,
    Boolean showExamplesFirst,
    Integer timeLimitSeconds,
    Integer itemCount,
    NavigationPolicy navigationPolicy,
    DefaultItemType defaultItemType,
    Map<String, Object> stimulusPhase,
    Map<String, Object> config,
    SectionStatus status
) {}