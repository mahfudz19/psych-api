package com.psycorp.psychapi.feature.test.api.dto.request;

import java.time.Instant;

public record StartSectionRequest(
    Stimulus stimulus
) {
    public record Stimulus(
        Instant startedAt,
        Instant deadlineAt
    ) {}
}
