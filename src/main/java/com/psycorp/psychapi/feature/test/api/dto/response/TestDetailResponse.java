package com.psycorp.psychapi.feature.test.api.dto.response;

import java.util.List;
import java.util.Map;

import com.psycorp.psychapi.feature.test.models.TestDefinition;

public record TestDetailResponse(
    TestDefinitionResponse test,
    Map<String, Object> stats,
    List<Map<String, Object>> progress
) {
    @SuppressWarnings("unchecked")
    public static TestDetailResponse fromMap(Map<String, Object> map) {
        return new TestDetailResponse(
            TestDefinitionResponse.fromEntity((TestDefinition) map.get("test")),
            (Map<String, Object>) map.get("stats"),
            (List<Map<String, Object>>) map.get("progress")
        );
    }
}