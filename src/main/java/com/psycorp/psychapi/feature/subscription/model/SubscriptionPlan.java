package com.psycorp.psychapi.feature.subscription.model;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection = "subscription_plans")
public class SubscriptionPlan extends PanacheMongoEntity {
    private String name; // e.g., "Pro Individual", "Business Org"
    private String code; // e.g., "PRO_IND", "BIZ_ORG" (Unik untuk integrasi Payment Gateway)
    private Double price; 
    private Integer durationDays; // e.g., 30 (Bulanan), 365 (Tahunan)
    
    // Target market: Apakah paket ini untuk dibeli oleh "USER" atau "ORGANIZATION"?
    private TargetAudience targetAudience; 
    
    // Batasan fitur
    private Integer maxSeats; // Null jika trial, atau angka (e.g., 50 untuk Org Plan)

    // === CONSTRUCTOR ===
    public SubscriptionPlan() {}

    // === GETTERS ===
    public ObjectId getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public Double getPrice() { return price; }
    public Integer getDurationDays() { return durationDays; }
    public TargetAudience getTargetAudience() { return targetAudience; }
    public Integer getMaxSeats() { return maxSeats; }

    // === SETTERS ===
    public void setName(String name) { this.name = name; }
    public void setCode(String code) { this.code = code; }
    public void setPrice(Double price) { this.price = price; }
    public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }
    public void setTargetAudience(TargetAudience targetAudience) { this.targetAudience = targetAudience; }
    public void setMaxSeats(Integer maxSeats) { this.maxSeats = maxSeats; }

    // === ENUMS ===
    public enum TargetAudience {
        USER("USER"),
        ORGANIZATION("ORGANIZATION");

        private final String value;
        TargetAudience(String value) { this.value = value; }

        @JsonValue
        public String getValue() { return value; }

        @JsonCreator
        public static TargetAudience fromValue(String value) {
            if (value == null || value.isBlank()) return null;
            for (TargetAudience type : values()) {
                if (type.value.equalsIgnoreCase(value)) return type;
            }
            throw new IllegalArgumentException("Invalid TargetAudience: " + value);
        }
    }
}