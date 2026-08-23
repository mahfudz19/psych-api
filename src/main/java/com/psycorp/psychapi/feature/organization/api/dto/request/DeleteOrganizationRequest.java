package com.psycorp.psychapi.feature.organization.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DeleteOrganizationRequest(
    @NotBlank(message = "Confirmation is required")
    String confirmation
) {}
