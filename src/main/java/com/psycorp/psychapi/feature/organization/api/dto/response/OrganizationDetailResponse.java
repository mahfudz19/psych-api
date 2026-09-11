package com.psycorp.psychapi.feature.organization.api.dto.response;

import java.time.Instant;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.psycorp.psychapi.feature.organization.model.Organization;
import com.psycorp.psychapi.feature.user.model.User;

@Schema(description = "Detail lengkap organization beserta daftar members")
@JsonInclude(Include.NON_NULL)
public record OrganizationDetailResponse(
    @Schema(description = "ID organization", examples = "507f1f77bcf86cd799439011")
    String id,

    @Schema(description = "Nama organization", examples = "PT Example Corp")
    String name,

    @Schema(description = "Deskripsi organization", examples = "Leading provider of widgets")
    String description,

    @Schema(description = "Website organization", examples = "https://www.example.com")
    String website,

    @Schema(description = "Nomor telepon organization", examples = "+628123456789")
    String phone,

    @Schema(description = "Email organization", examples = "info@example.com")
    String email,

    @Schema(description = "Alamat organization", examples = "Jl. Sudirman No. 1, Jakarta")
    String address,

    @Schema(description = "Logo URL", examples = "https://example.com/logo.png")
    String logo,

    @Schema(description = "ID owner organization", examples = "507f1f77bcf86cd799439011")
    String ownerId,

    @Schema(description = "Plan subscription", examples = "pro")
    String plan,

    @Schema(description = "Status verifikasi organization")
    Boolean status,

    @Schema(description = "Total seats tersedia", examples = "50")
    Integer seats,

    @Schema(description = "Seats yang sudah terpakai", examples = "12")
    Integer seatsUsed,

    @Schema(description = "Waktu pembuatan organization", examples = "2024-01-01T00:00:00Z")
    Instant createdAt,

    @Schema(description = "Waktu update terakhir", examples = "2024-06-15T10:30:00Z")
    Instant updatedAt,

    @Schema(description = "Daftar members organization")
    List<MemberSummary> members
) {
    @Schema(description = "Ringkasan informasi member organization")
    public record MemberSummary(
        @Schema(description = "ID user", examples = "507f1f77bcf86cd799439011")
        String id,

        @Schema(description = "Nama lengkap", examples = "John Doe")
        String fullName,

        @Schema(description = "Email", examples = "john@example.com")
        String email,

        @Schema(description = "URL profile picture", examples = "https://example.com/profile.jpg")
        String profilePicture,

        @Schema(description = "Role dalam organization: owner, admin, member", examples = "admin")
        User.OrganizationRole organizationRole,

        @Schema(description = "Status akun: ACTIVE, PENDING, SUSPENDED", examples = "ACTIVE")
        String status,

        @Schema(description = "Waktu login terakhir", examples = "2024-06-15T10:30:00Z")
        Instant lastLoginAt
    ) {
        public static MemberSummary fromEntity(User user) {
            return new MemberSummary(
                user.getId() != null ? user.getId().toHexString() : null,
                user.getFullName(),
                user.getEmail(),
                user.getProfilePicture(),
                user.getOrganizationRole(),
                user.getStatus() != null ? user.getStatus().getValue() : null,
                user.getLastLoginAt()
            );
        }
    }

    public static OrganizationDetailResponse of(Organization org, List<User> members) {
        return new OrganizationDetailResponse(
            org.getId() != null ? org.getId().toHexString() : null,
            org.getName(),
            org.getDescription(),
            org.getWebsite(),
            org.getPhone(),
            org.getEmail(),
            org.getAddress(),
            org.getLogo(),
            org.getOwnerId() != null ? org.getOwnerId().toHexString() : null,
            org.getPlan(),
            org.getStatus(),
            org.getSeats(),
            org.getSeatsUsed(),
            org.getCreatedAt(),
            org.getUpdatedAt(),
            members.stream().map(MemberSummary::fromEntity).toList()
        );
    }
}