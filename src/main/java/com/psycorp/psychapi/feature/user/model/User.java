package com.psycorp.psychapi.feature.user.model;

import java.time.Instant;
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
    private Provider provider; // "google", "facebook", "local"
    private String providerId; // "google_id", "facebook_id", null for local
    private String fullName; // "John"
    private String profilePicture; // "https://example.com/profile.jpg"
    private String phone; // "+1234567890"
    private String bio; // "Hello, I'm John Doe!"
    private String dateOfBirth; // "1990-01-01"
    private Gender gender; // "male", "female"

    // === SYSTEM ROLES ===
    private List<Role> roles; // ["USER"], ["ORGANIZATION"]
    
    // === ORGANIZATION RELATIONSHIP ===
    private ObjectId organizationId; // FK to organizations (nullable)
    private OrganizationRole organizationRole; // "owner", "admin", "member"
    private String organizationName; // Denormalized

    // === SUBSCRIPTION & MONETIZATION ===
    private Integer revenueSharePercentage; // 0-100 (default: 0)

    // === REFERRAL SYSTEM ===
    private String referralCode;            // Unique code untuk user ini (e.g., "JOHN2024", "REF_ABC123")
    private ObjectId referredBy;            // ObjectId user yang mereferensikan
    private List<ObjectId> referralIds;     // List ObjectId user yang direferensikan
    private Integer totalReferrals;         // Total referrals count (denormalized untuk performa)
    private Integer successfulReferrals;    // Referrals yang completed registration
    private Double referralEarnings;        // Total earnings dari referrals (dalam currency atau credits)
    private Instant referredAt;             // Kapan user ini direferensikan

    // === ORGANIZATION INVITATION ===
    private String inviteCode;              // Unique code untuk user ini (e.g., "JOHN2024", "REF_ABC123")
    private ObjectId invitedBy;             // ObjectId user yang invite ke organization
    private ObjectId invitedOrganizationId; // Organization yang diinvite untuk join
    private InvitationStatus invitationStatus; // "pending", "accepted", "declined", "expired"
    private Instant invitationSentAt;
    private Instant invitationAcceptedAt;
    private OrganizationRole invitationRole;          // Role yang ditawarkan ("member", "admin", "owner")

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

    // === PASSWORD RESET SYSTEM ===
    private String resetPasswordToken;
    private Instant resetPasswordExpiresAt;

    public User() {}

    // === GETTERS ===
    
    public ObjectId getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public Provider getProvider() { return provider; }
    public String getProviderId() { return providerId; }
    public String getFullName() { return fullName; }
    public String getProfilePicture() { return profilePicture; }
    public String getPhone() { return phone; }
    public String getBio() { return bio; }
    public String getDateOfBirth() { return dateOfBirth; }
    public Gender getGender() { return gender; }
    public List<Role> getRoles() { return roles; }
    public ObjectId getOrganizationId() { return organizationId; }
    public OrganizationRole getOrganizationRole() { return organizationRole; }
    public String getOrganizationName() { return organizationName; }
    public Integer getRevenueSharePercentage() { return revenueSharePercentage; }
    
    // Referral System Getters
    public String getReferralCode() { return referralCode; }
    public ObjectId getReferredBy() { return referredBy; }
    public List<ObjectId> getReferralIds() { return referralIds; }
    public Integer getTotalReferrals() { return totalReferrals; }
    public Integer getSuccessfulReferrals() { return successfulReferrals; }
    public Double getReferralEarnings() { return referralEarnings; }
    public Instant getReferredAt() { return referredAt; }
    
    // Organization Invitation Getters
    public String getInviteCode() { return inviteCode; }
    public ObjectId getInvitedBy() { return invitedBy; }
    public ObjectId getInvitedOrganizationId() { return invitedOrganizationId; }
    public InvitationStatus getInvitationStatus() { return invitationStatus; }
    public Instant getInvitationSentAt() { return invitationSentAt; }
    public Instant getInvitationAcceptedAt() { return invitationAcceptedAt; }
    public OrganizationRole getInvitationRole() { return invitationRole; }
    
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

    // Letakkan di area GETTERS
    public String getResetPasswordToken() { return resetPasswordToken; }
    public Instant getResetPasswordExpiresAt() { return resetPasswordExpiresAt; }

    // === SETTERS ===
    
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setProvider(Provider provider) { this.provider = provider; }
    public void setProviderId(String providerId) { this.providerId = providerId; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setBio(String bio) { this.bio = bio; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public void setGender(Gender gender) { this.gender = gender; }
    public void setRoles(List<Role> roles) { this.roles = roles; }
    public void setOrganizationId(ObjectId organizationId) { this.organizationId = organizationId; }
    public void setOrganizationRole(OrganizationRole organizationRole) { this.organizationRole = organizationRole; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }
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
    public void setInvitationStatus(InvitationStatus invitationStatus) { this.invitationStatus = invitationStatus; }
    public void setInvitationSentAt(Instant invitationSentAt) { this.invitationSentAt = invitationSentAt; }
    public void setInvitationAcceptedAt(Instant invitationAcceptedAt) { this.invitationAcceptedAt = invitationAcceptedAt; }
    public void setInvitationRole(OrganizationRole invitationRole) { this.invitationRole = invitationRole; }
    
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

    // Letakkan di area SETTERS
    public void setResetPasswordToken(String resetPasswordToken) { this.resetPasswordToken = resetPasswordToken; }
    public void setResetPasswordExpiresAt(Instant resetPasswordExpiresAt) { this.resetPasswordExpiresAt = resetPasswordExpiresAt; }

    // === ENUMS ===

    public enum Role {
        USER("USER"),
        ORGANIZATION("ORGANIZATION"),
        SUPERADMIN("SUPERADMIN");

        private final String value;
        Role(String value) { this.value = value; }

        @JsonValue
        public String getValue() { return value; }

        @JsonCreator
        public static Role fromValue(String value) {
            if (value == null || value.isBlank()) return null;
            for (Role role : values()) {
                if (role.value.equalsIgnoreCase(value)) return role;
            }
            throw new IllegalArgumentException("Invalid Role: " + value);
        }
    }

    public enum OrganizationRole {
        member("member"),
        admin("admin"),
        owner("owner");

        private final String value;
        OrganizationRole(String value) { this.value = value; }

        @JsonValue
        public String getValue() { return value; }

        @JsonCreator
        public static OrganizationRole fromValue(String value) {
            if (value == null || value.isBlank()) return null;
            for (OrganizationRole role : values()) {
                if (role.value.equalsIgnoreCase(value)) return role;
            }
            throw new IllegalArgumentException("Invalid OrganizationRole: " + value);
        }
    }

    public enum InvitationStatus {
        pending("pending"),
        accepted("accepted"),
        declined("declined"),
        expired("expired");

        private final String value;
        InvitationStatus(String value) { this.value = value; }

        @JsonValue
        public String getValue() { return value; }

        @JsonCreator
        public static InvitationStatus fromValue(String value) {
            if (value == null || value.isBlank()) return null;
            for (InvitationStatus status : values()) {
                if (status.value.equalsIgnoreCase(value)) return status;
            }
            throw new IllegalArgumentException("Invalid InvitationStatus: " + value);
        }
    }

    public enum Provider {
        local("local"),
        google("google"),
        facebook("facebook");

        private final String value;
        Provider(String value) { this.value = value; }

        @JsonValue
        public String getValue() { return value; }

        @JsonCreator
        public static Provider fromValue(String value) {
            if (value == null || value.isBlank()) return null;
            for (Provider p : values()) {
                if (p.value.equalsIgnoreCase(value)) return p;
            }
            throw new IllegalArgumentException("Invalid Provider: " + value);
        }
    }

    public enum Gender {
        male("male"),
        female("female");

        private final String value;
        Gender(String value) { this.value = value; }

        @JsonValue
        public String getValue() { return value; }

        @JsonCreator
        public static Gender fromValue(String value) {
            if (value == null || value.isBlank()) return null;
            for (Gender g : values()) {
                if (g.value.equalsIgnoreCase(value)) return g;
            }
            throw new IllegalArgumentException("Invalid Gender: " + value);
        }
    }

    public enum AccountType {
        INDIVIDUAL("INDIVIDUAL"),
        ORGANIZATION("ORGANIZATION");
        
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

    public void executeUpdate(Bson update) {
        Bson updateWithTimestamp = Updates.combine(
            update,
            Updates.set("updatedAt", Instant.now())
        );

        User.mongoCollection().updateOne(Filters.eq("_id", this.id), updateWithTimestamp);
    }
}
