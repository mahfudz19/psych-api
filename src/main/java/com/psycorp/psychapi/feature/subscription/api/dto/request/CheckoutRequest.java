package com.psycorp.psychapi.feature.subscription.api.dto.request;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

public record CheckoutRequest(
    @NotBlank(message = "Plan code is required")
    @Schema(description = "Kode paket yang akan dibeli", examples = "ORG_PRO")
    String planCode
) {}