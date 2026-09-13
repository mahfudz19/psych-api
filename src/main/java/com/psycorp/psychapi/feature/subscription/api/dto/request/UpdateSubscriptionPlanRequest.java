package com.psycorp.psychapi.feature.subscription.api.dto.request;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.psycorp.psychapi.feature.subscription.model.SubscriptionPlan.TargetAudience;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateSubscriptionPlanRequest(
    @Size(max = 100, message = "Name must not exceed 100 characters")
    @Schema(description = "Nama paket langganan", example = "Pro Individual")
    String name,

    @Size(max = 50, message = "Code must not exceed 50 characters")
    @Schema(description = "Kode unik untuk integrasi payment gateway", example = "PRO_IND")
    String code,

    @Min(value = 0, message = "Price cannot be negative")
    @Schema(description = "Harga paket", example = "150000.0")
    Double price,

    @Min(value = 1, message = "Duration must be at least 1 day")
    @Schema(description = "Durasi paket dalam hari", example = "30")
    Integer durationDays,

    @Schema(description = "Target market paket (USER atau ORGANIZATION)", example = "USER")
    TargetAudience targetAudience,

    @Schema(description = "Batas maksimal kursi (null jika unlimited/tidak berlaku)", example = "50")
    Integer maxSeats
) {}