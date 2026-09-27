package com.psycorp.psychapi.feature.subscription.api.dto.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.psycorp.psychapi.feature.subscription.model.Subscription;
import com.psycorp.psychapi.feature.subscription.model.Transaction;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransactionHistoryResponse(
    String referenceId,
    String planId,
    Double amount,
    String paymentMethod,
    Transaction.Status status,
    Instant createdAt,
    Instant paidAt,
    Instant expiredAt,
    SubscriptionDetail subscription
) {
    public record SubscriptionDetail(
        Subscription.Status status,
        Instant startDate,
        Instant endDate,
        Instant canceledAt
    ) {}

    public static TransactionHistoryResponse fromEntity(Transaction transaction, Subscription subscription) {
        SubscriptionDetail subDetail = null;
        if (subscription != null) {
            subDetail = new SubscriptionDetail(
                subscription.getStatus(),
                subscription.getStartDate(),
                subscription.getEndDate(),
                subscription.getCanceledAt()
            );
        }

        return new TransactionHistoryResponse(
            transaction.getReferenceId(),
            transaction.getPlanId() != null ? transaction.getPlanId().toHexString() : null,
            transaction.getAmount(),
            transaction.getPaymentMethod(),
            transaction.getStatus(),
            transaction.getCreatedAt(),
            transaction.getPaidAt(),
            transaction.getExpiredAt(),
            subDetail
        );
    }
}