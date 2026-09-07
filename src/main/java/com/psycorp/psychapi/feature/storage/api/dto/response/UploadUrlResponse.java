package com.psycorp.psychapi.feature.storage.api.dto.response;

public record UploadUrlResponse(
    String uploadUrl,    // GCS Signed URL (PUT)
    String fileKey,      // temp/users/{userId}/profile/{uuid}.jpg
    String bucket        // nama bucket yang digunakan
) {}