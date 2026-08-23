package com.psycorp.psychapi.feature.auth.api.dto.request;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Combined request DTO untuk login/refresh endpoint.
 * Supports dual-function: login dengan email/password ATAU refresh dengan refreshToken.
 */
@Schema(description = "Request payload gabungan untuk login atau refresh token. Gunakan email+password untuk login, atau refreshToken saja untuk refresh token.")
@JsonInclude(Include.NON_NULL)
public record AuthRequest(
    @Schema(description = "Email address user (untuk login)", examples = "individual.free@example.com")
    String email,
    
    @Schema(description = "Password user (untuk login)", examples = "password123")
    String password
) {}
