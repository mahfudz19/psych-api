package com.psycorp.psychapi.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

/**
 * DTO untuk Organization Members API.
 * Berisi request records untuk list, invite, update role, dan remove members.
 */
public final class OrganizationMemberRequests {

    private OrganizationMemberRequests() {}

    /**
     * Query parameters untuk GET /organizations/:orgId/members.
     * Mendukung pagination, search, sort, dan custom filter.
     */
    public record MembersListRequest(
        @QueryParam("search")
        @Parameter(description = "Search keyword untuk fullName atau email", example = "john")
        String search,

        @QueryParam("filter")
        @Parameter(description = "Custom filter dengan format field:operator:value", example = "organizationRole:in:admin,member")
        String filter,

        @QueryParam("page")
        @DefaultValue("1")
        @Parameter(description = "Page number", example = "1")
        int page,

        @QueryParam("limit")
        @DefaultValue("10")
        @Parameter(description = "Items per page", example = "10")
        int limit,

        @QueryParam("sortBy")
        @DefaultValue("createdAt")
        @Parameter(description = "Sort field", example = "createdAt")
        String sortBy,

        @QueryParam("sortOrder")
        @DefaultValue("desc")
        @Parameter(description = "Sort order (asc/desc)", example = "desc")
        String sortOrder
    ) {}

    /**
     * Request body untuk invite member baru ke organization.
     */
    public record InviteMemberRequest(
        @Schema(
            description = "Email user yang diinvite",
            examples = "newmember@example.com",
            required = true,
            format = "email"
        )
        @NotBlank(message = "Email is required")
        @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Invalid email format")
        String email,

        @Schema(
            description = "Role yang ditawarkan ke member yang diinvite",
            examples = "member",
            required = true
        )
        @NotBlank(message = "Role is required")
        @Pattern(regexp = "^(member|admin)$", message = "Role must be either 'member' or 'admin'")
        String role,

        @Schema(
            description = "Pesan personal untuk invitation",
            examples = "Welcome to our team!",
            required = false,
            maxLength = 500
        )
        @Size(max = 500, message = "Message must not exceed 500 characters")
        String message
    ) {}

    /**
     * Request body untuk update role member.
     */
    public record UpdateMemberRoleRequest(
        @Schema(
            description = "Role baru untuk member",
            examples = "admin",
            required = true
        )
        @NotBlank(message = "Role is required")
        @Pattern(regexp = "^(member|admin)$", message = "Role must be either 'member' or 'admin'")
        String role
    ) {}

    /**
     * Request body untuk remove member dari organization.
     */
    public record RemoveMemberRequest(
        @Schema(
            description = "Confirmation text untuk remove member. Harus 'REMOVE_MEMBER'",
            examples = "REMOVE_MEMBER",
            required = true
        )
        @NotBlank(message = "Confirmation is required")
        String confirmation
    ) {}
}
