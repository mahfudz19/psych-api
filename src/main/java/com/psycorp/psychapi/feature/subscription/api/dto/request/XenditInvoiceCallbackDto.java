package com.psycorp.psychapi.feature.subscription.api.dto.request;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.psycorp.psychapi.feature.subscription.model.Transaction;

public class XenditInvoiceCallbackDto {
    
    @Schema(description = "ID referensi transaksi di sistem kita", examples = "TXN-12345")
    @JsonProperty("external_id")
    public String externalId;

    @Schema(description = "Status pembayaran dari Xendit", examples = "PAID")
    public Transaction.Status status;

    @Schema(description = "ID invoice dari Xendit", examples = "inv_123456789")
    public String id;
    
    @Schema(description = "Jumlah yang dibayarkan", examples = "150000")
    public Double amount;
}
