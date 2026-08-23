package com.psycorp.psychapi.feature.organization.api.dto.response;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.psycorp.psychapi.feature.user.model.User;

/**
 * Response DTO untuk Organization Member.
 * Digunakan untuk mengembalikan data member organization dalam format yang aman dan terstruktur.
 */
@Schema(description = "Response untuk organization member")
public record OrganizationMemberResponse(
    @Schema(description = "ID user", examples = "507f1f77bcf86cd799439011")
    String id,
    
    @Schema(description = "Email user", examples = "member@example.com")
    String email,
    
    @Schema(description = "Nama lengkap user", examples = "John Doe")
    String fullName,
    
    @Schema(description = "Role user dalam organization", examples = "admin")
    String organizationRole,
    
    @Schema(description = "Status user", examples = "ACTIVE")
    String status
) {
    
    /**
     * Factory method untuk membuat OrganizationMemberResponse dari User entity.
     * 
     * @param user User entity
     * @return OrganizationMemberResponse DTO
     */
    public static OrganizationMemberResponse fromEntity(User user) {
        return new OrganizationMemberResponse(
            user.getId() != null ? user.getId().toHexString() : null,
            user.getEmail(),
            user.getFullName(),
            user.getOrganizationRole(),
            user.getStatus() != null ? user.getStatus().getValue() : null
        );
    }
}
