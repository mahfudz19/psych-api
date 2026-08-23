package com.psycorp.psychapi.feature.user.api.dto.response;

import java.time.Instant;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.psycorp.psychapi.feature.user.model.User;

/**
 * Response DTO untuk informasi user.
 */
@Schema(description = "Informasi lengkap user")
@JsonInclude(Include.NON_NULL)
public record UserResponse(
    @Schema(description = "Unique ID user", examples = "507f1f77bcf86cd799439011")
    String id,
    
    @Schema(description = "Email address user", examples = "user@example.com")
    String email,
    
    @Schema(description = "Nama lengkap user", examples = "John Doe")
    String fullName,
    
    @Schema(description = "URL profile picture user", examples = "https://example.com/profile.jpg")
    String profilePicture,
    
    @Schema(description = "Nomor telepon user", examples = "+628123456789")
    String phone,
    
    @Schema(description = "Bio atau deskripsi singkat user", examples = "Software Developer")
    String bio,
    
    @Schema(description = "Tanggal lahir user (YYYY-MM-DD)", examples = "1990-01-15")
    String dateOfBirth,
    
    @Schema(description = "Jenis kelamin user", examples = "MALE")
    String gender,
    
    @Schema(description = "Daftar roles user", examples = "[\"USER\", \"ADMIN\"]")
    List<String> roles,
    
    @Schema(description = "ID organisasi user", examples = "507f191e810c19729de860ea")
    String organizationId,
    
    @Schema(description = "Role user dalam organisasi", examples = "MEMBER")
    String organizationRole,
    
    @Schema(description = "Nama organisasi user", examples = "PT Example Corp")
    String organizationName,
    
    @Schema(description = "Tier subscription user", examples = "PREMIUM")
    String subscriptionTier,
    
    @Schema(description = "Waktu expiry subscription", examples = "2025-12-31T23:59:59Z")
    Instant subscriptionExpiry,
    
    @Schema(description = "Kode referral user", examples = "REF123456")
    String referralCode,
    
    @Schema(description = "Total jumlah referral", examples = "10")
    Integer totalReferrals,
    
    @Schema(description = "Jumlah referral yang berhasil", examples = "5")
    Integer successfulReferrals,
    
    @Schema(description = "Total earnings dari referral", examples = "50000.00")
    Double referralEarnings,
    
    @Schema(description = "Kode undangan user", examples = "INV123456")
    String inviteCode,
    
    @Schema(description = "Status undangan user", examples = "pending")
    String invitationStatus,
    
    @Schema(description = "Role undangan user", examples = "ADMIN")
    String invitationRole,
    
    @Schema(description = "Status akun user", examples = "ACTIVE")
    User.Status status,
    
    @Schema(description = "Waktu login terakhir", examples = "2024-01-15T10:30:00Z")
    Instant lastLoginAt,
    
    @Schema(description = "Waktu pembuatan akun", examples = "2024-01-01T00:00:00Z")
    Instant createdAt,
    
    @Schema(description = "Waktu update terakhir", examples = "2024-01-15T10:30:00Z")
    Instant updatedAt,
    
    @Schema(description = "Tipe akun", examples = "PERSONAL")
    String accountType
) {
    /**
     * Factory method untuk convert dari User entity ke UserResponse.
     * @param user User entity
     * @return UserResponse
     */
    public static UserResponse fromEntity(com.psycorp.psychapi.feature.user.model.User user) {
        return new UserResponse(
            user.getId() != null ? user.getId().toHexString() : null,
            user.getEmail(),
            user.getFullName(),
            user.getProfilePicture(),
            user.getPhone(),
            user.getBio(),
            user.getDateOfBirth(),
            user.getGender(),
            user.getRoles(),
            user.getOrganizationId() != null ? user.getOrganizationId().toHexString() : null,
            user.getOrganizationRole(),
            user.getOrganizationName(),
            user.getSubscriptionTier(),
            user.getSubscriptionExpiry(),
            user.getReferralCode(),
            user.getTotalReferrals(),
            user.getSuccessfulReferrals(),
            user.getReferralEarnings(),
            user.getInviteCode(),
            user.getInvitationStatus(),
            user.getInvitationRole(),
            user.getStatus(),
            user.getLastLoginAt(),
            user.getCreatedAt(),
            user.getUpdatedAt(),
            user.getAccountType() != null ? user.getAccountType().name() : null
        );
    }
}
