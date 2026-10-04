package com.psycorp.psychapi.feature.subscription.models;

import java.time.Instant;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

@JsonInclude(JsonInclude.Include.NON_NULL)
@MongoEntity(collection = "transactions")
public class Transaction extends PanacheMongoEntity {
    
    // Identitas Tagihan
    private String referenceId;       // ID Unik internal kita (misal: INV-20260915-XYZ)
    private String xenditInvoiceId;   // ID balasan dari Xendit
    private String checkoutUrl;       // URL UI Xendit (jika ingin dipakai sebagai fallback)
    
    private ObjectId subscriberId;
    private Subscription.SubscriberType subscriberType;

    // Relasi Bisnis
    private ObjectId organizationId;  // Siapa yang beli
    private ObjectId planId;          // Beli paket apa
    
    // Uang & Metode
    private Double amount;
    private String paymentMethod;     // BCA_VA, QRIS, dll (Terisi setelah user bayar)
    
    // State Machine
    private Status status;            // PENDING, PAID, EXPIRED
    
    private Instant createdAt;
    private Instant paidAt;
    private Instant expiredAt;

    public Transaction() {}

    public enum Status {
        PENDING("PENDING"),
        PAID("PAID"),
        EXPIRED("EXPIRED"),
        FAILED("FAILED");

        private final String value;

        Status(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @JsonCreator
        public static Status fromValue(String value) {
            if (value == null || value.isBlank()) return null;
            for (Status status : values()) {
                if (status.value.equalsIgnoreCase(value)) return status;
            }
            throw new IllegalArgumentException("Invalid Transaction Status: " + value);
        }
    }

    // === GETTERS ===
    public ObjectId getId() { return id; }
    public String getReferenceId() { return referenceId; }
    public String getXenditInvoiceId() { return xenditInvoiceId; }
    public String getCheckoutUrl() { return checkoutUrl; }
    public ObjectId getSubscriberId() { return subscriberId; }
    public Subscription.SubscriberType getSubscriberType() { return subscriberType; }
    public ObjectId getOrganizationId() { return organizationId; }
    public ObjectId getPlanId() { return planId; }
    public Double getAmount() { return amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPaidAt() { return paidAt; }
    public Instant getExpiredAt() { return expiredAt; }

    // === SETTERS ===
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    public void setXenditInvoiceId(String xenditInvoiceId) { this.xenditInvoiceId = xenditInvoiceId; }
    public void setCheckoutUrl(String checkoutUrl) { this.checkoutUrl = checkoutUrl; }
    public void setSubscriberId(ObjectId subscriberId) { this.subscriberId = subscriberId; }
    public void setSubscriberType(Subscription.SubscriberType subscriberType) { this.subscriberType = subscriberType; }
    public void setOrganizationId(ObjectId organizationId) { this.organizationId = organizationId; }
    public void setPlanId(ObjectId planId) { this.planId = planId; }
    public void setAmount(Double amount) { this.amount = amount; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public void setStatus(Status status) { this.status = status; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
    public void setExpiredAt(Instant expiredAt) { this.expiredAt = expiredAt; }
}