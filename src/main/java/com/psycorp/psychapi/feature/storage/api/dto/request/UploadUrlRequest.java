package com.psycorp.psychapi.feature.storage.api.dto.request;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request payload untuk generate GCS Signed URL upload")
public record UploadUrlRequest(
    @Schema(description = "Nama file yang akan diupload", examples = "profile.jpg", required = true)
    @NotBlank(message = "Filename is required")
    String filename,

    @Schema(description = "MIME type file", examples = "image/jpeg", required = true)
    @NotBlank(message = "MIME type is required")
    String mimeType,

    @Schema(description = "Kategori file: PROFILE_PICTURE, DOCUMENT", examples = "PROFILE_PICTURE", required = true)
    @NotBlank(message = "Category is required")
    String category,

    @Schema(description = "Visibility file: PUBLIC atau PRIVATE", examples = "PUBLIC", required = true)
    @NotBlank(message = "Visibility is required")
    String visibility
) {
    public static final String DESCRIPTION = "Generate GCS Signed URL untuk upload file. Client upload langsung ke URL ini menggunakan HTTP PUT.";
}