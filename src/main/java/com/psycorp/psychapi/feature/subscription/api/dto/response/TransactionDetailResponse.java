package com.psycorp.psychapi.feature.subscription.api.dto.response;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.psycorp.psychapi.feature.subscription.model.Transaction;

public record TransactionDetailResponse(
    @Schema(description = "ID referensi transaksi", examples = "INV-12345678")
    String referenceId,
    
    @Schema(description = "URL fallback", examples = "https://checkout.xendit.co/web/...")
    String checkoutUrl,
    
    @Schema(description = "Status transaksi", examples = "PENDING")
    Transaction.Status status,

    @Schema(description = "Daftar Virtual Account yang tersedia")
    Object availableBanks,

    @Schema(description = "Daftar QRIS yang tersedia")
    Object availableQrCodes,

    @Schema(description = "Daftar E-Wallet yang tersedia")
    Object availableEwallets
) {}