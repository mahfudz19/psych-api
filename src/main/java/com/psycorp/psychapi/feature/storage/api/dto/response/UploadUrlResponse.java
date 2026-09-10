package com.psycorp.psychapi.feature.storage.api.dto.response;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@Schema(description = "Response payload berisi Signed URL untuk upload file ke GCS")
@JsonInclude(Include.NON_NULL)
public record UploadUrlResponse(
    @Schema(description = "GCS Signed URL untuk upload (HTTP PUT)", examples = "https://storage.googleapis.com/bucket/temp/users/123/profile/uuid.jpg?X-Goog-Signature=...")
    String uploadUrl,

    @Schema(description = "File key path di GCS", examples = "temp/users/507f1f77bcf86cd799439011/profile/550e8400-e29b-41d4-a716-446655440000.jpg")
    String fileKey,

    @Schema(description = "Nama GCS bucket", examples = "psycorp-storage")
    String bucket
) {}