package com.psycorp.psychapi.domain.model;

import java.time.Instant;
import java.util.List;

import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

@JsonInclude(JsonInclude.Include.NON_NULL)
@MongoEntity(collection = "users")
public class User extends PanacheMongoEntity {
    private String email; // "john.doe@example.com"
    private String password; // "hashed_password"
    private String provider; // "google", "facebook", "local"
    private String providerId; // "google_id", "facebook_id", null for local
    private String fullName; // "John"
    private String profilePicture; // "https://example.com/profile.jpg"
    private String phone; // "+1234567890"
    private String bio; // "Hello, I'm John Doe!"
    private String dateOfBirth; // "1990-01-01"
    private String gender; // "male", "female"

    // === SYSTEM ROLES ===
    private List<String> roles; // ["USER"], ["ADMIN"], ["ORGANIZATION"]
    
    // === ORGANIZATION RELATIONSHIP ===
    private ObjectId organizationId; // FK to organizations (nullable)
    private String organizationRole; // "owner", "admin", "member"
    private String organizationName; // Denormalized

    // === SUBSCRIPTION & MONETIZATION ===
    private String subscriptionTier; // "free", "premium", "enterprise"
    private Instant subscriptionExpiry; // "2026-08-15T08:00:00Z +00:00"
    private Integer revenueSharePercentage; // 0-100 (default: 0)

    // === REFERRAL SYSTEM ===
    private String referredBy; // User yang mengajak/menginvite user ini. FK to users (nullable)
    private List<String> referrals; // List user yang diinvite oleh user ini.

    // === ACCOUNT STATUS ===
    private String status; // "active", "inactive", "suspended"
    private Instant lastLoginAt; // "2024-08-15T08:00:00Z +00:00"
    private Integer loginAttempts; // Number of failed login attempts

    private Instant createdAt; // "2024-08-15T08:00:00Z +00:00"
    private Instant updatedAt; // "2024-08-15T08:00:00Z +00:00"
    private Instant deletedAt; // "2024-08-15T08:00:00Z +00:00" (nullable)

    public User() {}

    // === GETTERS ===
    
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getProvider() { return provider; }
    public String getProviderId() { return providerId; }
    public String getFullName() { return fullName; }
    public String getProfilePicture() { return profilePicture; }
    public String getPhone() { return phone; }
    public String getBio() { return bio; }
    public String getDateOfBirth() { return dateOfBirth; }
    public String getGender() { return gender; }
    public List<String> getRoles() { return roles; }
    public ObjectId getOrganizationId() { return organizationId; }
    public String getOrganizationRole() { return organizationRole; }
    public String getOrganizationName() { return organizationName; }
    public String getSubscriptionTier() { return subscriptionTier; }
    public Instant getSubscriptionExpiry() { return subscriptionExpiry; }
    public Integer getRevenueSharePercentage() { return revenueSharePercentage; }
    public String getReferredBy() { return referredBy; }
    public List<String> getReferrals() { return referrals; }
    public String getStatus() { return status; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public Integer getLoginAttempts() { return loginAttempts; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }

    // === SETTERS ===
    
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setProvider(String provider) { this.provider = provider; }
    public void setProviderId(String providerId) { this.providerId = providerId; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setBio(String bio) { this.bio = bio; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public void setGender(String gender) { this.gender = gender; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    public void setOrganizationId(ObjectId organizationId) { this.organizationId = organizationId; }
    public void setOrganizationRole(String organizationRole) { this.organizationRole = organizationRole; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }
    public void setSubscriptionTier(String subscriptionTier) { this.subscriptionTier = subscriptionTier; }
    public void setSubscriptionExpiry(Instant subscriptionExpiry) { this.subscriptionExpiry = subscriptionExpiry; }
    public void setRevenueSharePercentage(Integer revenueSharePercentage) { this.revenueSharePercentage = revenueSharePercentage; }
    public void setReferredBy(String referredBy) { this.referredBy = referredBy; }
    public void setReferrals(List<String> referrals) { this.referrals = referrals; }
    public void setStatus(String status) { this.status = status; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public void setLoginAttempts(Integer loginAttempts) { this.loginAttempts = loginAttempts; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }

    public static User create(String email, String password, String fullName, String referredBy) {
        User user = new User();
        
        // WAJIB
        user.email = email;
        user.password = password;
        user.fullName = fullName;
        user.provider = "local";
        user.roles = List.of("USER");
        user.status = "active";
        user.subscriptionTier = "free";
        user.revenueSharePercentage = 0;
        user.createdAt = Instant.now();
        user.updatedAt = Instant.now();
        user.loginAttempts = 0;
        
        // OPSIONAL
        if (referredBy != null && !referredBy.isEmpty()) {
            user.referredBy = referredBy;
        }
        
        return user;
    }
    
    public void executeUpdate(Bson update) {
        Bson updateWithTimestamp = Updates.combine(
            update,
            Updates.set("updatedAt", Instant.now())
        );

        User.mongoCollection().updateOne(Filters.eq("_id", this.id), updateWithTimestamp);
    }


    public void updateProfile(String fullName, String phone, String bio) {
        if (fullName != null) this.fullName = fullName;
        if (phone != null) this.phone = phone;
        if (bio != null) this.bio = bio;
        this.updatedAt = Instant.now();
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
        this.status = "deleted";
        this.updatedAt = Instant.now();
    }
}
