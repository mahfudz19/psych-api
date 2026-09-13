package com.psycorp.psychapi.feature.subscription.model;

import java.time.Instant;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection = "subscriptions")
public class Subscription extends PanacheMongoEntity {
    private ObjectId planId; 
    
    // POLIMORFISME: Siapa yang berlangganan?
    private SubscriberType subscriberType; // "USER" atau "ORGANIZATION"
    private ObjectId subscriberId; // FK ke users atau organizations tergantung subscriberType
    
    private Instant startDate;
    private Instant endDate;
    
    private Status status; // "ACTIVE", "CANCELED", "EXPIRED", "PAYMENT_PENDING"
    
    private String paymentGatewayId; // ID transaksi dari Midtrans/Stripe
    
    // === CONSTRUCTOR ===
    public Subscription() {}

    // === GETTERS ===
    public ObjectId getId() { return id; }
    public ObjectId getPlanId() { return planId; }
    public SubscriberType getSubscriberType() { return subscriberType; }
    public ObjectId getSubscriberId() { return subscriberId; }
    public Instant getStartDate() { return startDate; }
    public Instant getEndDate() { return endDate; }
    public Status getStatus() { return status; }
    public String getPaymentGatewayId() { return paymentGatewayId; }

    // === SETTERS ===
    public void setPlanId(ObjectId planId) { this.planId = planId; }
    public void setSubscriberType(SubscriberType subscriberType) { this.subscriberType = subscriberType; }
    public void setSubscriberId(ObjectId subscriberId) { this.subscriberId = subscriberId; }
    public void setStartDate(Instant startDate) { this.startDate = startDate; }
    public void setEndDate(Instant endDate) { this.endDate = endDate; }
    public void setStatus(Status status) { this.status = status; }
    public void setPaymentGatewayId(String paymentGatewayId) { this.paymentGatewayId = paymentGatewayId; }

    // === ENUMS ===
    public enum SubscriberType {
        USER("USER"),
        ORGANIZATION("ORGANIZATION");

        private final String value;
        SubscriberType(String value) { this.value = value; }

        @JsonValue
        public String getValue() { return value; }

        @JsonCreator
        public static SubscriberType fromValue(String value) {
            if (value == null || value.isBlank()) return null;
            for (SubscriberType type : values()) {
                if (type.value.equalsIgnoreCase(value)) return type;
            }
            throw new IllegalArgumentException("Invalid SubscriberType: " + value);
        }
    }

    public enum Status {
        ACTIVE("ACTIVE"),               // Sedang berjalan normal
        CANCELED("CANCELED"),           // Dibatalkan sebelum waktunya oleh user
        EXPIRED("EXPIRED"),             // Habis masa aktifnya
        PAYMENT_PENDING("PAYMENT_PENDING"); // Menunggu pembayaran gateway

        private final String value;
        Status(String value) { this.value = value; }

        @JsonValue
        public String getValue() { return value; }

        @JsonCreator
        public static Status fromValue(String value) {
            if (value == null || value.isBlank()) return null;
            for (Status status : values()) {
                if (status.value.equalsIgnoreCase(value)) return status;
            }
            throw new IllegalArgumentException("Invalid Subscription Status: " + value);
        }
    }
}