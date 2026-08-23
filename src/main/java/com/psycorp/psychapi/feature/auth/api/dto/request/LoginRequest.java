package com.psycorp.psychapi.feature.auth.api.dto.request;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO untuk login user.
 */
@Schema(description = "Request payload untuk login user")
public record LoginRequest(
    @Schema(description = "Email address user yang terdaftar", examples = "individual.free@example.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,
    
    @Schema(description = "Password user (minimal 8 karakter)", examples = "password123")
    @NotBlank(message = "Password is required")
    String password
) {}
