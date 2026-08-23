package com.psycorp.psychapi.feature.auth.api.dto.request;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO untuk refresh token.
 */
@Schema(description = "Request payload untuk refresh access token")
public record RefreshTokenRequest(
    @Schema(description = "Refresh token yang didapat dari response login/refresh sebelumnya", 
            examples = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")
    @NotBlank(message = "Refresh token is required")
    String refreshToken
) {}
