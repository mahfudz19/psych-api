package com.psycorp.psychapi.feature.organization.api.dto.response;

import java.time.Instant;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.psycorp.psychapi.feature.user.model.User;

@Schema(description = "Detail lengkap member organization untuk admin/owner view")
@JsonInclude(Include.NON_NULL)
public record OrganizationMemberDetailResponse(
    @Schema(description = "ID user", examples = "507f1f77bcf86cd799439011")
    String id,

    @Schema(description = "Email", examples = "member@example.com")
    String email,

    @Schema(description = "Nama lengkap", examples = "John Doe")
    String fullName,

    @Schema(description = "URL profile picture", examples = "https://example.com/profile.jpg")
    String profilePicture,

    @Schema(description = "Nomor telepon", examples = "+628123456789")
    String phone,

    @Schema(description = "Bio", examples = "Software Developer")
    String bio,

    @Schema(description = "Jenis kelamin", examples = "MALE")
    User.Gender gender,

    @Schema(description = "Role dalam organization", examples = "admin")
    User.OrganizationRole organizationRole,

    @Schema(description = "Status akun", examples = "ACTIVE")
    String status,

    @Schema(description = "Tipe akun", examples = "ORGANIZATION")
    String accountType,

    @Schema(description = "Subscription tier", examples = "premium")
    String subscriptionTier,

    @Schema(description = "Status undangan", examples = "accepted")
    User.InvitationStatus invitationStatus,

    @Schema(description = "Role undangan awal", examples = "member")
    User.OrganizationRole invitationRole,

    @Schema(description = "Waktu login terakhir", examples = "2024-06-15T10:30:00Z")
    Instant lastLoginAt,

    @Schema(description = "Waktu bergabung (pembuatan akun)", examples = "2024-01-01T00:00:00Z")
    Instant createdAt
) {
    public static OrganizationMemberDetailResponse fromEntity(User user) {
        return new OrganizationMemberDetailResponse(
            user.getId() != null ? user.getId().toHexString() : null,
            user.getEmail(),
            user.getFullName(),
            user.getProfilePicture(),
            user.getPhone(),
            user.getBio(),
            user.getGender(),
            user.getOrganizationRole(),
            user.getStatus() != null ? user.getStatus().getValue() : null,
            user.getAccountType() != null ? user.getAccountType().getValue() : null,
            user.getSubscriptionTier(),
            user.getInvitationStatus(),
            user.getInvitationRole(),
            user.getLastLoginAt(),
            user.getCreatedAt()
        );
    }
}