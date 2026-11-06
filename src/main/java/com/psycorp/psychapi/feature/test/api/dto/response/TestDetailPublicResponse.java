package com.psycorp.psychapi.feature.test.api.dto.response;

import java.util.List;
import java.util.Map;

import com.psycorp.psychapi.feature.test.models.TestDefinition;
import com.psycorp.psychapi.feature.test.models.TestSection;

public record TestDetailPublicResponse(
    String id,
    String code,
    String name,
    String shortName,
    String category,
    String description,
    String instructions,
    String version,
    Integer estimatedMinutes,
    Integer totalItems,
    Integer totalDurationSeconds,
    String pausePolicy,
    String language,
    String thumbnailUrl,
    List<SectionSummary> sections,
    Map<String, Object> myAttempt  // { id, status } atau null
) {
    public record SectionSummary(
        String id,
        String code,
        Integer order,
        String name,
        Integer itemCount,
        Integer timeLimitSeconds,
        String navigationPolicy
    ) {
        public static SectionSummary fromEntity(TestSection s) {
            return new SectionSummary(
                s.getId().toHexString(),
                s.getCode(),
                s.getOrder(),
                s.getName(),
                s.getItemCount(),
                s.getTimeLimitSeconds(),
                s.getNavigationPolicy() != null ? s.getNavigationPolicy().getValue() : null
            );
        }
    }

    public static TestDetailPublicResponse fromEntity(TestDefinition t, List<TestSection> sections,
            Map<String, Object> myAttempt) {
        return new TestDetailPublicResponse(
            t.id.toHexString(),
            t.getCode(),
            t.getName(),
            t.getShortName(),
            t.getCategory() != null ? t.getCategory().getValue() : null,
            t.getDescription(),
            t.getInstructions(),
            t.getVersion(),
            t.getEstimatedMinutes(),
            t.getTotalItems(),
            t.getTotalDurationSeconds(),
            t.getPausePolicy() != null ? t.getPausePolicy().getValue() : null,
            t.getLanguage(),
            t.getThumbnailUrl(),
            sections.stream().map(SectionSummary::fromEntity).toList(),
            myAttempt
        );
    }
}
