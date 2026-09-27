package com.psycorp.psychapi.feature.subscription.api.dto.response;

import java.time.Instant;

public record TransactionStatusResponse(
    boolean hasActiveSubscription,
    boolean hasPendingTransaction,
    String pendingReferenceId,
    ActiveSubscriptionDetail activeSubscription
) {
    public record ActiveSubscriptionDetail(
        SubscriptionPlanResponse plan,
        Instant startDate,
        Instant endDate
    ) {}
}