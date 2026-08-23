package com.psycorp.psychapi.feature.user.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
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
    private List<String> roles; // ["USER"], ["ORGANIZATION"]
    
    // === ORGANIZATION RELATIONSHIP ===
    private ObjectId organizationId; // FK to organizations (nullable)
    private String organizationRole; // "owner", "admin", "member"
    private String organizationName; // Denormalized

    // === SUBSCRIPTION & MONETIZATION ===
    private String subscriptionTier; // "free", "premium", "enterprise"
    private Instant subscriptionExpiry; // "2026-08-15T08:00:00Z +00:00"
    private Integer revenueSharePercentage; // 0-100 (default: 0)

    // === REFERRAL SYSTEM ===
    private String referralCode;            // Unique code untuk user ini (e.g., "JOHN2024", "REF_ABC123")
    private ObjectId referredBy;            // ObjectId user yang mereferensikan
    private List<ObjectId> referralIds;     // List ObjectId user yang direferensikan
    private Integer totalReferrals;         // Total referrals count (denormalized untuk performa)
    private Integer successfulReferrals;    // Referrals yang completed registration
    private Double referralEarnings;        // Total earnings dari referrals (dalam currency atau credits)
    private Instant referredAt;             // Kapan user ini direferensikan
    private List<ReferralCodeHistoryEntry> referralCodeHistory;  // List history referral code yang di-archive

    // === ORGANIZATION INVITATION ===
    private String inviteCode;              // Unique code untuk user ini (e.g., "JOHN2024", "REF_ABC123")
    private ObjectId invitedBy;             // ObjectId user yang invite ke organization
    private ObjectId invitedOrganizationId; // Organization yang diinvite untuk join
    private String invitationStatus;        // "pending", "accepted", "declined", "expired"
    private Instant invitationSentAt;
    private Instant invitationAcceptedAt;
    private String invitationRole;          // Role yang ditawarkan ("member", "admin", "owner")

    // === ACCOUNT STATUS ===
    private Status status; // "active", "inactive", "suspended", "pending", "deleted"
    private Instant lastLoginAt; // "2024-08-15T08:00:00Z +00:00"
    private Integer loginAttempts; // Number of failed login attempts

    private Instant createdAt; // "2024-08-15T08:00:00Z +00:00"
    private Instant updatedAt; // "2024-08-15T08:00:00Z +00:00"
    private Instant deletedAt; // "2024-08-15T08:00:00Z +00:00" (nullable)

    // === VERIFICATION & TTL SYSTEM ===
    private String verificationToken;
    private Instant verificationExpiresAt;
    private Instant expiredAt;
    
    // === ACCOUNT TYPE ===
    private AccountType accountType; // INDIVIDUAL / ORGANIZATION

    public User() {}

    // === GETTERS ===
    
    public ObjectId getId() { return id; }
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
    
    // Referral System Getters
    public String getReferralCode() { return referralCode; }
    public ObjectId getReferredBy() { return referredBy; }
    public List<ObjectId> getReferralIds() { return referralIds; }
    public Integer getTotalReferrals() { return totalReferrals; }
    public Integer getSuccessfulReferrals() { return successfulReferrals; }
    public Double getReferralEarnings() { return referralEarnings; }
    public Instant getReferredAt() { return referredAt; }
    public List<ReferralCodeHistoryEntry> getReferralCodeHistory() { return referralCodeHistory; }
    public void setReferralCodeHistory(List<ReferralCodeHistoryEntry> referralCodeHistory) { this.referralCodeHistory = referralCodeHistory; }
    
    // Organization Invitation Getters
    public String getInviteCode() { return inviteCode; }
    public ObjectId getInvitedBy() { return invitedBy; }
    public ObjectId getInvitedOrganizationId() { return invitedOrganizationId; }
    public String getInvitationStatus() { return invitationStatus; }
    public Instant getInvitationSentAt() { return invitationSentAt; }
    public Instant getInvitationAcceptedAt() { return invitationAcceptedAt; }
    public String getInvitationRole() { return invitationRole; }
    
    public Status getStatus() { return status; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public Integer getLoginAttempts() { return loginAttempts; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public AccountType getAccountType() { return accountType; }

    // Verification System Getters
    public String getVerificationToken() { return verificationToken; }
    public Instant getVerificationExpiresAt() { return verificationExpiresAt; }
    public Instant getExpiredAt() { return expiredAt; }

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
    
    // Referral System Setters
    public void setReferralCode(String referralCode) { this.referralCode = referralCode; }
    public void setReferredBy(ObjectId referredBy) { this.referredBy = referredBy; }
    public void setReferralIds(List<ObjectId> referralIds) { this.referralIds = referralIds; }
    public void setTotalReferrals(Integer totalReferrals) { this.totalReferrals = totalReferrals; }
    public void setSuccessfulReferrals(Integer successfulReferrals) { this.successfulReferrals = successfulReferrals; }
    public void setReferralEarnings(Double referralEarnings) { this.referralEarnings = referralEarnings; }
    public void setReferredAt(Instant referredAt) { this.referredAt = referredAt; }
    
    // Organization Invitation Setters
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
    public void setInvitedBy(ObjectId invitedBy) { this.invitedBy = invitedBy; }
    public void setInvitedOrganizationId(ObjectId invitedOrganizationId) { this.invitedOrganizationId = invitedOrganizationId; }
    public void setInvitationStatus(String invitationStatus) { this.invitationStatus = invitationStatus; }
    public void setInvitationSentAt(Instant invitationSentAt) { this.invitationSentAt = invitationSentAt; }
    public void setInvitationAcceptedAt(Instant invitationAcceptedAt) { this.invitationAcceptedAt = invitationAcceptedAt; }
    public void setInvitationRole(String invitationRole) { this.invitationRole = invitationRole; }
    
    public void setStatus(Status status) { this.status = status; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public void setLoginAttempts(Integer loginAttempts) { this.loginAttempts = loginAttempts; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }

    // Verification System Setters
    public void setVerificationToken(String verificationToken) { this.verificationToken = verificationToken; }
    public void setVerificationExpiresAt(Instant verificationExpiresAt) { this.verificationExpiresAt = verificationExpiresAt; }
    public void setExpiredAt(Instant expiredAt) { this.expiredAt = expiredAt; }

    public enum AccountType {
        INDIVIDUAL("individual"),
        ORGANIZATION("organization");
        
        private final String value;
        
        AccountType(String value) {
            this.value = value;
        }
        
        @JsonValue
        public String getValue() {
            return value;
        }
        
        @JsonCreator
        public static AccountType fromValue(String value) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("AccountType value cannot be null or blank");
            }
            for (AccountType type : values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException(
                "Invalid AccountType: '%s'. Valid values: INDIVIDUAL, ORGANIZATION".formatted(value)
            );
        }
    }

    public enum Status {
        ACTIVE("ACTIVE"),
        INACTIVE("INACTIVE"),
        SUSPENDED("SUSPENDED"),
        PENDING("PENDING"),
        DELETED("DELETED");
        
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
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Status value cannot be null or blank");
            }
            for (Status status : values()) {
                if (status.value.equalsIgnoreCase(value)) {
                    return status;
                }
            }
            throw new IllegalArgumentException(
                "Invalid Status: '%s'. Valid values: ACTIVE, INACTIVE, SUSPENDED, PENDING, DELETED".formatted(value)
            );
        }
    }

    public static User create(String email, String password, String fullName, User referrer, User inviter, AccountType accountType, String hashedVerificationToken, Instant verificationExpiresAt) {
        User user = new User();
        
        // WAJIB
        user.email = email;
        user.password = password;
        user.fullName = fullName;
        user.provider = "local";
        user.roles = List.of("USER");
        
        user.status = Status.PENDING;
        user.expiredAt = Instant.now().plus(24, java.time.temporal.ChronoUnit.HOURS);
        user.verificationToken = hashedVerificationToken;
        user.verificationExpiresAt = verificationExpiresAt;
        
        user.subscriptionTier = "free";
        user.revenueSharePercentage = 0;
        user.createdAt = Instant.now();
        user.updatedAt = Instant.now();
        user.loginAttempts = 0;
        
        // Auto-generate referral code
        user.referralCode = generateReferralCode(email, user.createdAt);
        user.referralIds = new ArrayList<>();
        user.totalReferrals = 0;
        user.successfulReferrals = 0;
        user.referralEarnings = 0.0;
        
        // Set account type
        user.accountType = accountType;

        // Set roles dan organization info berdasarkan account type
        if (accountType == AccountType.ORGANIZATION) {
            user.setRoles(List.of("USER", "ORGANIZATION"));
            user.setOrganizationRole("owner");
            user.setInvitationStatus("accepted");
            user.setInvitationAcceptedAt(Instant.now());
        }
                
        // OPSIONAL - Set referral info jika ada referrer (sudah tervalidasi)
        if (referrer != null) {
            user.referredBy = referrer.id;  // Langsung ObjectId, bukan toHexString()
            user.referredAt = Instant.now();
        }
        
        // OPSIONAL - Set invitation info jika ada inviter (sudah tervalidasi)
        if (inviter != null) {
            user.invitedBy = inviter.id;  // Langsung ObjectId, bukan toHexString()
            user.invitedOrganizationId = inviter.getInvitedOrganizationId();
            user.invitationStatus = "accepted";
            user.invitationSentAt = Instant.now();
            user.invitationAcceptedAt = Instant.now();
            user.invitationRole = inviter.getInvitationRole() != null ?
                                  inviter.getInvitationRole() : "member";
        }
        
        return user;
    }

    public void renewVerification(String hashedNewToken, Instant newVerificationExpiresAt) {
        // 1. Update nilai di dalam memori objek Java
        this.verificationToken = hashedNewToken;
        this.verificationExpiresAt = newVerificationExpiresAt;
        this.expiredAt = Instant.now().plus(24, java.time.temporal.ChronoUnit.HOURS);
        this.updatedAt = Instant.now();

        // 2. Siapkan perintah update MongoDB menggunakan Updates builder
        Bson update = Updates.combine(
            Updates.set("verificationToken", this.verificationToken),
            Updates.set("verificationExpiresAt", this.verificationExpiresAt),
            Updates.set("expiredAt", this.expiredAt)
        );

        // 3. Eksekusi pembaruan ke database menggunakan metode executeUpdate yang sudah ada
        this.executeUpdate(update);
    }

    public void activateAccount() {
        // Update object di memory Java
        this.status = Status.ACTIVE;
        this.verificationToken = null;
        this.verificationExpiresAt = null;
        this.expiredAt = null;

        Bson update = Updates.combine(
            Updates.set("status", Status.ACTIVE.getValue()),
            Updates.unset("verificationToken"),
            Updates.unset("verificationExpiresAt"),
            Updates.unset("expiredAt")
        );

        this.executeUpdate(update);
    }

    private static String generateReferralCode(String email, Instant createdAt) {
        if (email == null || email.isEmpty()) {
            return "USR" + createdAt.getEpochSecond() + (int)(Math.random() * 1000);
        }
        
        // Extract first 3 alphabetic characters for prefix
        String alphaOnly = email.replaceAll("[^a-zA-Z]", "");
        String prefix = alphaOnly.substring(0, Math.min(3, alphaOnly.length())).toUpperCase();
        
        // Use last 5 digits of timestamp
        String timestamp = String.valueOf(createdAt.getEpochSecond());
        String timeSuffix = timestamp.length() > 5 ? timestamp.substring(timestamp.length() - 5) : timestamp;
        
        // Add 3-digit random number for uniqueness (000-999)
        String randomSuffix = String.format("%03d", (int)(Math.random() * 1000));
        
        return prefix + timeSuffix + randomSuffix;
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
        this.status = Status.DELETED;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Inner class untuk representasi entry history referral code.
     * Digunakan untuk tracking kode referral yang di-archive setelah regenerasi.
     */
    public static class ReferralCodeHistoryEntry {
        private String code;
        private Instant archivedAt;
        private String reason;
        private String replacedBy;
        
        public ReferralCodeHistoryEntry() {}
        
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        
        public Instant getArchivedAt() { return archivedAt; }
        public void setArchivedAt(Instant archivedAt) { this.archivedAt = archivedAt; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public String getReplacedBy() { return replacedBy; }
        public void setReplacedBy(String replacedBy) { this.replacedBy = replacedBy; }
    }
}
