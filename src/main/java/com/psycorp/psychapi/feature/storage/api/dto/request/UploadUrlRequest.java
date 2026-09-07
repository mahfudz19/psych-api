package com.psycorp.psychapi.feature.storage.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UploadUrlRequest(
    @NotBlank String filename,
    @NotBlank String mimeType,
    @NotBlank String category,    // PROFILE_PICTURE, DOCUMENT, dll
    @NotBlank String visibility   // PUBLIC atau PRIVATE
) {}