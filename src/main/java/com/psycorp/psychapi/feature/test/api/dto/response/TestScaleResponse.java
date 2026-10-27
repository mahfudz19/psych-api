package com.psycorp.psychapi.feature.test.api.dto.response;

import java.time.Instant;
import java.util.Map;

import com.psycorp.psychapi.feature.test.models.TestScale;
import com.psycorp.psychapi.feature.test.models.TestScale.ScaleRole;
import com.psycorp.psychapi.feature.test.models.TestScale.ScaleType;
import com.psycorp.psychapi.feature.test.models.TestScale.ScoreType;

public record TestScaleResponse(
    String id, String testId, String code, String name, ScaleType type,
    ScaleRole role, String parentScaleId, String sectionId, Integer order,
    String description, String lowLabel, String highLabel, Integer rawMin,
    Integer rawMax, ScoreType scoreType, Map<String, Object> composite,
    Boolean isReported, Boolean isValidityScale, String color,
    Instant createdAt, Instant updatedAt
) {
    public static TestScaleResponse fromEntity(TestScale s) {
        return new TestScaleResponse(
            s.getId().toHexString(),
            s.getTestId().toHexString(),
            s.getCode(), s.getName(), s.getType(), s.getRole(),
            s.getParentScaleId() != null ? s.getParentScaleId().toHexString() : null,
            s.getSectionId() != null ? s.getSectionId().toHexString() : null,
            s.getOrder(), s.getDescription(), s.getLowLabel(), s.getHighLabel(),
            s.getRawMin(), s.getRawMax(), s.getScoreType(), s.getComposite(),
            s.getIsReported(), s.getIsValidityScale(), s.getColor(),
            s.getCreatedAt(), s.getUpdatedAt()
        );
    }
}