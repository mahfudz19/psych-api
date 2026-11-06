package com.psycorp.psychapi.feature.test.api.dto.request;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotNull;

public record SaveAnswersRequest(
    @NotNull(message = "answers wajib diisi") List<Map<String, Object>> answers,
    Integer revision
) {}
