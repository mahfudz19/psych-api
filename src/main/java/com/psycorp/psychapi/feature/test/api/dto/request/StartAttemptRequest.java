package com.psycorp.psychapi.feature.test.api.dto.request;

import org.bson.types.ObjectId;
import jakarta.validation.constraints.NotNull;

public record StartAttemptRequest(
    @NotNull(message = "testId wajib diisi") ObjectId testId
) {}
