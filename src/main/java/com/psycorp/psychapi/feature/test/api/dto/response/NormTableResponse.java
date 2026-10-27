package com.psycorp.psychapi.feature.test.api.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.psycorp.psychapi.feature.test.models.NormTable;
import com.psycorp.psychapi.feature.test.models.NormTable.NormMethod;
import com.psycorp.psychapi.feature.test.models.NormTable.NormStatus;
import com.psycorp.psychapi.feature.test.models.TestScale;

public record NormTableResponse(
    String id,
    String testId,
    String testVersion,
    String scaleId,
    String sectionId,
    String normVersion,
    String name,
    String targetScoreType,
    NormMethod method,
    Map<String, Object> criteria,
    List<Map<String, Object>> entries,
    Double mean,
    Double sd,
    List<Map<String, Object>> categoryCutoffs,
    Integer sampleSize,
    String source,
    Instant validFrom,
    Instant validUntil,
    NormStatus status,
    Map<String, Object> coverage,
    Instant createdAt,
    Instant updatedAt
) {
    public static NormTableResponse fromEntity(NormTable norm) {
        return fromEntity(norm, null, null);
    }

    public static NormTableResponse fromEntity(NormTable norm, TestScale scale,
            Map<String, Object> coverage) {
        return new NormTableResponse(
                norm.id.toHexString(),
                norm.getTestId() != null ? norm.getTestId().toHexString() : null,
                norm.getTestVersion(),
                norm.getScaleId() != null ? norm.getScaleId().toHexString() : null,
                norm.getSectionId() != null ? norm.getSectionId().toHexString() : null,
                norm.getNormVersion(),
                norm.getName(),
                norm.getTargetScoreType(),
                norm.getMethod(),
                norm.getCriteria(),
                norm.getEntries(),
                norm.getMean(),
                norm.getSd(),
                norm.getCategoryCutoffs(),
                norm.getSampleSize(),
                norm.getSource(),
                norm.getValidFrom(),
                norm.getValidUntil(),
                norm.getStatus(),
                coverage,
                norm.getCreatedAt(),
                norm.getUpdatedAt());
    }
}
