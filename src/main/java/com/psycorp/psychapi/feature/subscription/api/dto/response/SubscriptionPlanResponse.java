package com.psycorp.psychapi.feature.subscription.api.dto.response;

import java.time.Instant;

import com.psycorp.psychapi.feature.subscription.models.SubscriptionPlan;
import com.psycorp.psychapi.feature.subscription.models.SubscriptionPlan.TargetAudience;

public record SubscriptionPlanResponse(
    String id,
    String name,
    String code,
    Double price,
    Integer durationDays,
    TargetAudience targetAudience,
    Integer maxSeats,
    Boolean recommended,
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
            plan.getRecommended(),
            plan.getCreatedAt(),
            plan.getUpdatedAt()
        );
    }
}