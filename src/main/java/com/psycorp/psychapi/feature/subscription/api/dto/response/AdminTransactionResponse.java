package com.psycorp.psychapi.feature.subscription.api.dto.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.psycorp.psychapi.feature.subscription.models.Subscription;
import com.psycorp.psychapi.feature.subscription.models.Transaction;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdminTransactionResponse(
    // Identitas Pelaku
    String subscriberId,
    Subscription.SubscriberType subscriberType,
    String subscriberEmail, // Diambil dari User
    String subscriberName,  // Diambil dari User/Organization

    // Detail Paket & Harga
    String planId,
    String planCode,        // Diambil dari SubscriptionPlan
    String planName,        // Diambil dari SubscriptionPlan
    Double amount,
    String currency,

    // Detail Pembayaran & Gateway
    String referenceId,
    String xenditInvoiceId,
    String checkoutUrl,
    String paymentMethod,

    // Waktu & Status
    Transaction.Status status,
    Instant createdAt,
    Instant paidAt,
    Instant expiredAt,

    // Relasi Langganan
    SubscriptionDetail subscription
) {
    public record SubscriptionDetail(
        String subscriptionId,
        Subscription.Status status,
        Instant startDate,
        Instant endDate,
        Instant canceledAt
    ) {}
}