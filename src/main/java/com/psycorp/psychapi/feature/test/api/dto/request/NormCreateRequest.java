package com.psycorp.psychapi.feature.test.api.dto.request;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

import com.psycorp.psychapi.feature.test.models.NormTable.NormMethod;

import jakarta.validation.constraints.NotNull;

public record NormCreateRequest(
    @NotNull(message = "scaleId wajib diisi") ObjectId scaleId,

    @NotNull(message = "normVersion wajib diisi") String normVersion,

    @NotNull(message = "name wajib diisi") String name,

    @NotNull(message = "targetScoreType wajib diisi") String targetScoreType,

    @NotNull(message = "method wajib diisi") NormMethod method,

    // { ageMin, ageMax, gender, education }
    Map<String, Object> criteria,

    List<Map<String, Object>> entries,

    Double mean,
    Double sd,

    List<Map<String, Object>> categoryCutoffs,

    Integer sampleSize,
    String source,
    Instant validFrom,
    Instant validUntil
) {}
