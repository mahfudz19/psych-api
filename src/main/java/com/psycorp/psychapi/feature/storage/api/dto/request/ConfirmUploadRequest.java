package com.psycorp.psychapi.feature.storage.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ConfirmUploadRequest(
    @NotBlank String fileKey,     // temp/users/{userId}/profile/{uuid}.jpg
    @NotBlank String bucket       // psych-public-assets atau psych-private-assets
) {}