package com.psycorp.psychapi.domain.model;

import java.time.Instant;

import org.bson.types.ObjectId;

import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection = "organizations")
public class Organization {
    private String name; // "Acme Corp"
    private String description; // "Leading provider of widgets"
    private String website; // "https://www.acmecorp.com"
    private String logo; // "https://www.acmecorp.com/logo.png"
    private String address; // "123 Main St, Anytown, USA"
    private String phone; // "+1 (123) 456-7890"
    private String email; // "X6Z1W@example.com"

    // === STATUS & APPROVAL ===
    private Boolean status; // "unverified", "verified", "enterprise"
    private String approvedBy; // "admin_user_id" (nullable)
    private Instant approvedAt; // "2024-08-15T08:00:00Z +00:00" (nullable)
    private String rejectionReason; // "Reason for rejection" (nullable)
    private Instant trialStartsAt; // "2024-08-15T08:00:00Z +00:00" (nullable)
    private Instant trialEndsAt; // "2024-08-15T08:00:00Z +00:00" (nullable)

    // === PLAN & BILLING ===
    private String plan; // "free_trial", "free", "pro", "enterprise"
    private Instant subscriptionExpiry; // "2024-08-15T08:00:00Z +00:00" (nullable)
    private Integer seats; // 5 (free), 50 (pro), unlimited (enterprise)
    private Integer seatsUsed; // 3 (free), 40 (pro), unlimited (enterprise)

    // === OWNERSHIP ===
    private ObjectId ownerId; // FK to users (nullable)

    private Instant createdAt; // "2024-08-15T08:00:00Z +00:00"
    private Instant updatedAt; // "2024-08-15T08:00:00Z +00:00"
    private Instant deletedAt; // "2024-08-15T08:00:00Z +00:00" (nullable)

    public Organization() {}

    // === GETTERS ===
    
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getWebsite() { return website; }
    public String getLogo() { return logo; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public Boolean getStatus() { return status; }
    public String getApprovedBy() { return approvedBy; }
    public Instant getApprovedAt() { return approvedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public Instant getTrialStartsAt() { return trialStartsAt; }
    public Instant getTrialEndsAt() { return trialEndsAt; }
    public String getPlan() { return plan; }
    public Instant getSubscriptionExpiry() { return subscriptionExpiry; }
    public Integer getSeats() { return seats; }
    public Integer getSeatsUsed() { return seatsUsed; }
    public ObjectId getOwnerId() { return ownerId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }

    // === SETTERS ===
    
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setWebsite(String website) { this.website = website; }
    public void setLogo(String logo) { this.logo = logo; }
    public void setAddress(String address) { this.address = address; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setEmail(String email) { this.email = email; }
    public void setStatus(Boolean status) { this.status = status; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public void setTrialStartsAt(Instant trialStartsAt) { this.trialStartsAt = trialStartsAt; }
    public void setTrialEndsAt(Instant trialEndsAt) { this.trialEndsAt = trialEndsAt; }
    public void setPlan(String plan) { this.plan = plan; }
    public void setSubscriptionExpiry(Instant subscriptionExpiry) { this.subscriptionExpiry = subscriptionExpiry; }
    public void setSeats(Integer seats) { this.seats = seats; }
    public void setSeatsUsed(Integer seatsUsed) { this.seatsUsed = seatsUsed; }
    public void setOwnerId(ObjectId ownerId) { this.ownerId = ownerId; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
