package com.psycorp.psychapi.feature.test.api.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.psycorp.psychapi.feature.test.models.TestSection;
import com.psycorp.psychapi.feature.test.models.TestSection.DefaultItemType;
import com.psycorp.psychapi.feature.test.models.TestSection.NavigationPolicy;
import com.psycorp.psychapi.feature.test.models.TestSection.SectionStatus;

public record TestSectionResponse(
    String id, String testId, String testVersion, String code, Integer order,
    String name, String instructions, List<Map<String, Object>> examples,
    Boolean showExamplesFirst, Integer timeLimitSeconds, Integer itemCount,
    NavigationPolicy navigationPolicy, DefaultItemType defaultItemType,
    List<String> scaleIds, Map<String, Object> stimulusPhase, Map<String, Object> config,
    SectionStatus status, Instant createdAt, Instant updatedAt
) {
    public static TestSectionResponse fromEntity(TestSection s) {
        return new TestSectionResponse(
            s.getId().toHexString(),
            s.getTestId().toHexString(),
            s.getTestVersion(),
            s.getCode(), s.getOrder(), s.getName(), s.getInstructions(),
            s.getExamples(), s.getShowExamplesFirst(), s.getTimeLimitSeconds(),
            s.getItemCount(), s.getNavigationPolicy(), s.getDefaultItemType(),
            s.getScaleIds() == null ? List.of() : s.getScaleIds().stream().map(id -> id.toHexString()).toList(),
            s.getStimulusPhase(), s.getConfig(), s.getStatus(),
            s.getCreatedAt(), s.getUpdatedAt()
        );
    }
}