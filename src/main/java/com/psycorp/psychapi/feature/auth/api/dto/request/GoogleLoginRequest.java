package com.psycorp.psychapi.feature.auth.api.dto.request;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request payload untuk login menggunakan Google SSO")
public record GoogleLoginRequest(
    @Schema(description = "Google ID Token dari frontend", examples = "eyJhbGciOiJSUzI1NiIs...")
    @NotBlank(message = "Token is required")
    String token
) {}