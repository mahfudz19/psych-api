package com.psycorp.psychapi.feature.organization.api.dto.request;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import com.psycorp.psychapi.shared.request.PageableRequest;

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

    // === CONSTANT UNTUK SHARED DESCRIPTIONS ===
    public static final String[] SEARCH_FIELDS = {"fullName", "email"};

    
    public static final String MEMBERS_LIST_DESCRIPTION = """
        Mengambil daftar semua members dalam organization dengan pagination, search, sort, dan filter.
        
        ### Authentication Required
        Endpoint ini memerlukan JWT token yang valid.
        
        ### Authorization
        Hanya owner atau admin organization yang bisa melihat semua members.
        
        ### Query Parameters
        - **search**: Search keyword untuk fullName dan email
        - **filter**: Custom filter dengan format `field:operator:value`
        - **page**: Page number (default: 1)
        - **limit**: Items per page (default: 10)
        - **sortBy**: Sort field (default: createdAt)
        - **sortOrder**: Sort order (default: desc)
        
        ### Filter Examples
        - `role:in:admin,member`
        - `status:eq:active`
        - `createdAt:gte:2024-01-01`
        """;
    
    public static final String MEMBER_DETAIL_DESCRIPTION = """
        Mengambil detail member organization berdasarkan user ID.
        
        ### Authorization
        Owner dan admin bisa melihat detail semua member. Member biasa hanya bisa melihat detail diri sendiri.
        """;
    
    public static final String UPDATE_ROLE_DESCRIPTION = """
        Update role member organization.
        
        ### Authorization
        Hanya owner organization yang bisa update role member.
        
        ### Restrictions
        - Tidak bisa mengubah role owner
        - Role baru harus "member" atau "admin"
        """;
    
    public static final String REMOVE_MEMBER_DESCRIPTION = """
        Remove member dari organization.
        
        ### Authorization
        Owner dan admin bisa remove member.
        
        ### Restrictions
        - Owner organization tidak bisa di-remove
        - Admin tidak bisa remove admin lain
        """;
    
    public static final String JOIN_ORGANIZATION_DESCRIPTION = """
        Member join organization.
        
        ### Restrictions
        - Member atau admin bisa join
        """;

    public static final String LEAVE_ORGANIZATION_DESCRIPTION = """
        Member meninggalkan organization.
        
        ### Restrictions
        - Owner tidak bisa leave organization
        - Member atau admin bisa leave
        """;

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
        List<String> filter,

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
    ) implements PageableRequest {}

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
    ) {
        public static final String DESCRIPTION = """
            Request body untuk update role member organization.
            
            ### Authorization
            Hanya owner organization yang bisa update role member.
            
            ### Restrictions
            - Tidak bisa mengubah role owner
            - Role baru harus "member" atau "admin"
            
            **Contoh:**
            ```json
            {
                "role": "admin"
            }
            ```
            """;
    }

}
