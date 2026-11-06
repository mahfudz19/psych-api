package com.psycorp.psychapi.feature.test.api.dto.response;

import com.psycorp.psychapi.feature.test.models.TestDefinition;

public record TestCatalogResponse(
    String id,
    String code,
    String name,
    String shortName,
    String category,
    String description,
    Integer estimatedMinutes,
    Integer totalItems,
    String thumbnailUrl,
    String language
) {
    public static TestCatalogResponse fromEntity(TestDefinition t) {
        return new TestCatalogResponse(
            t.id.toHexString(),
            t.getCode(),
            t.getName(),
            t.getShortName(),
            t.getCategory() != null ? t.getCategory().getValue() : null,
            t.getDescription(),
            t.getEstimatedMinutes(),
            t.getTotalItems(),
            t.getThumbnailUrl(),
            t.getLanguage()
        );
    }
}
