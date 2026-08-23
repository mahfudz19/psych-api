package com.psycorp.psychapi.feature.auth.api.dto.response;

import java.time.Instant;
import java.util.List;

import org.bson.types.ObjectId;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.psycorp.psychapi.feature.user.model.User;

/**
 * Response DTO untuk informasi user yang sedang login.
 */
@Schema(description = "Informasi lengkap user yang sedang login")
@JsonInclude(Include.NON_NULL)
public record UserInfoResponse(
    @Schema(description = "Unique ID user", examples = "507f1f77bcf86cd799439011")
    ObjectId id,
    
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
    
    @Schema(description = "Daftar roles user", examples = "[\"USER\", \"ADMIN\"]")
    List<String> roles,
    
    @Schema(description = "ID organisasi user", examples = "507f191e810c19729de860ea")
    ObjectId organizationId,
    
    @Schema(description = "Role user dalam organisasi", examples = "MEMBER")
    String organizationRole,
    
    @Schema(description = "Nama organisasi user", examples = "PT Example Corp")
    String organizationName,
    
    @Schema(description = "Tier subscription user", examples = "PREMIUM")
    String subscriptionTier,
    
    @Schema(description = "Tipe akun", examples = "INDIVIDUAL")
    User.AccountType accountType,
    
    @Schema(description = "Status akun user", examples = "ACTIVE")
    User.Status status,

    @Schema(description = "Waktu login terakhir", examples = "2024-01-15T10:30:00Z")
    Instant lastLoginAt,
    
    @Schema(description = "Waktu pembuatan akun", examples = "2024-01-01T00:00:00Z")
    Instant createdAt
) {
    /**
     * Factory method untuk membuat UserInfoResponse dari User entity.
     * @param user User entity
     * @return UserInfoResponse
     */
    public static UserInfoResponse from(com.psycorp.psychapi.feature.user.model.User user) {
        return new UserInfoResponse(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getProfilePicture(),
            user.getPhone(),
            user.getBio(),
            user.getRoles(),
            user.getOrganizationId(),
            user.getOrganizationRole(),
            user.getOrganizationName(),
            user.getSubscriptionTier(),
            user.getAccountType(),
            user.getStatus(),
            user.getLastLoginAt(),
            user.getCreatedAt()
        );
    }
}
