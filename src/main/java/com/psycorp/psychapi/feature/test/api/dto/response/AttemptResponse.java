package com.psycorp.psychapi.feature.test.api.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.psycorp.psychapi.feature.test.models.TestAttempt;
import com.psycorp.psychapi.feature.test.models.TestDefinition;

public record AttemptResponse(
    String id,
    String testId,
    String testVersion,
    String testCode,
    String testName,
    Integer attemptNumber,
    String status,
    Instant startedAt,
    Instant completedAt,
    String currentSectionId,
    List<Map<String, Object>> sections,  // sectionStates + answeredCount
    Instant serverTime,
    TestDefinition.PausePolicy pausePolicy
) {
    public static AttemptResponse fromEntity(TestAttempt a, List<Map<String, Object>> sections, TestDefinition t) {
        return new AttemptResponse(
            a.id.toHexString(),
            a.getTestId() != null ? a.getTestId().toHexString() : null,
            a.getTestVersion(),
            a.getTestCode(),
            a.getTestName(),
            a.getAttemptNumber(),
            a.getStatus() != null ? a.getStatus().getValue() : null,
            a.getStartedAt(),
            a.getCompletedAt(),
            a.getCurrentSectionId() != null ? a.getCurrentSectionId().toHexString() : null,
            sections,
            Instant.now(),
            t.getPausePolicy()
        );
    }
}
