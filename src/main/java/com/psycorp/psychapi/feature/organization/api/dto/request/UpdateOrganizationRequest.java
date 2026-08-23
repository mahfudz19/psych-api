package com.psycorp.psychapi.feature.organization.api.dto.request;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateOrganizationRequest(
    @Schema(description = "Name of the organization", examples = "My Organization")
    @Size(max = 100, message = "Organization name must not exceed 100 characters")
    String name,

    @Schema(description = "Description of the organization", examples = "This is a sample organization")
    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description,

    @Schema(description = "Website user yang terdaftar", examples = "https://example.com")
    String website,

    @Schema(description = "Phone number user yang terdaftar", examples = "+6281234567890")
    String phone,

    @Schema(description = "Email address user yang terdaftar", examples = "contact@usaha-gratis.example.com")
    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Invalid email format")
    String email,

    @Schema(description = "Address user yang terdaftar", examples = "Jl. Contoh No. 123, Kota Contoh")
    @Size(max = 300, message = "Address must not exceed 300 characters")
    String address
) {}
