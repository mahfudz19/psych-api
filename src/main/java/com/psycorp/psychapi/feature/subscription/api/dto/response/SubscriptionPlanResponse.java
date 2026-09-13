package com.psycorp.psychapi.feature.subscription.api.dto.response;

import java.time.Instant;

import com.psycorp.psychapi.feature.subscription.model.SubscriptionPlan;
import com.psycorp.psychapi.feature.subscription.model.SubscriptionPlan.TargetAudience;

public record SubscriptionPlanResponse(
    String id,
    String name,
    String code,
    Double price,
    Integer durationDays,
    TargetAudience targetAudience,
    Integer maxSeats,
    Instant createdAt,
    Instant updatedAt
) {
    public static SubscriptionPlanResponse fromEntity(SubscriptionPlan plan) {
        return new SubscriptionPlanResponse(
            plan.getId().toHexString(),
            plan.getName(),
            plan.getCode(),
            plan.getPrice(),
            plan.getDurationDays(),
            plan.getTargetAudience(),
            plan.getMaxSeats(),
            plan.getCreatedAt(),
            plan.getUpdatedAt()
        );
    }
}