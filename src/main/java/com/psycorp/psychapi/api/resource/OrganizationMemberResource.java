package com.psycorp.psychapi.api.resource;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.psycorp.psychapi.api.dto.OrganizationMemberRequests.MembersListRequest;
import com.psycorp.psychapi.api.dto.OrganizationMemberRequests.UpdateMemberRoleRequest;
import com.psycorp.psychapi.common.helper.ResponseHelper;
import com.psycorp.psychapi.common.response.ApiResponse;
import com.psycorp.psychapi.common.response.PaginationMeta;
import com.psycorp.psychapi.domain.model.User;
import com.psycorp.psychapi.domain.service.OrganizationMemberService;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/api/v1/organizations/{orgId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Organization Members", description = "API untuk mengelola members organization")
public class OrganizationMemberResource {

    @Inject
    OrganizationMemberService memberService;

    @GET
    @Path("/members")
    @RolesAllowed("USER")
    @SecurityRequirement(name = "Bearer")
    @Operation(
        summary = "Get all organization members",
        description = """
            Mengambil daftar semua members dalam organization dengan pagination, search, sort, dan filter.
            
            ### Authentication Required
            Endpoint ini memerlukan JWT token yang valid di cookie `__session` atau Authorization header.
            
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
            """
    )
    @APIResponse(
        responseCode = "200",
        description = "Members retrieved successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiResponse.class)
        )
    )
    @APIResponse(
        responseCode = "403",
        description = "Forbidden - Only owner or admin can view all members"
    )
    @APIResponse(
        responseCode = "404",
        description = "Organization not found"
    )
    public Response getMembers(
            @PathParam("orgId") String orgId,
            @BeanParam @Valid MembersListRequest request,
            @Context SecurityContext securityContext) {
        String userId = getUserIdFromSecurityContext(securityContext);

        List<User> members = memberService.getMembersByOrganizationId(orgId, userId, request);
        long total = memberService.getMembersCount(orgId, request.search(), request.filter());
        int totalPages = (int) Math.ceil((double) total / request.limit());

        PaginationMeta meta = new PaginationMeta(
            request.page(),
            request.limit(),
            total,
            totalPages
        );

        return ResponseHelper.ok(members, "Members retrieved successfully", meta);
    }

    @GET
    @Path("/members/{memberId}")
    @RolesAllowed("USER")
    @SecurityRequirement(name = "Bearer")
    @Operation(
        summary = "Get member detail",
        description = """
            Mengambil detail member organization berdasarkan user ID.
            
            ### Authorization
            Owner dan admin bisa melihat detail semua member. Member biasa hanya bisa melihat detail diri sendiri.
            """
    )
    @APIResponse(
        responseCode = "200",
        description = "Member retrieved successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiResponse.class)
        )
    )
    @APIResponse(
        responseCode = "403",
        description = "Forbidden - User is not a member of this organization"
    )
    @APIResponse(
        responseCode = "404",
        description = "Member or organization not found"
    )
    public Response getMemberById(
            @PathParam("orgId") String orgId,
            @PathParam("memberId") String memberId,
            @Context SecurityContext securityContext) {
        String userId = getUserIdFromSecurityContext(securityContext);

        User member = memberService.getMemberById(orgId, memberId, userId);

        return ResponseHelper.ok(member, "Member retrieved successfully");
    }

    /**
     * Update role member organization.
     * Hanya owner yang bisa update role member.
     *
     * @param orgId Organization ID
     * @param memberId User ID member
     * @param request Update role request
     * @param securityContext Security context
     * @return Response dengan data member yang sudah diupdate
     */
    @PATCH
    @Path("/members/{memberId}/role")
    @RolesAllowed("USER")
    @SecurityRequirement(name = "Bearer")
    @Operation(
        summary = "Update member role",
        description = """
            Update role member organization.
            
            ### Authorization
            Hanya owner organization yang bisa update role member.
            
            ### Restrictions
            - Tidak bisa mengubah role owner
            - Role baru harus "member" atau "admin"
            """
    )
    @RequestBody(
        description = "Update member role request",
        required = true,
        content = @Content(
            schema = @Schema(implementation = UpdateMemberRoleRequest.class),
            examples = {
                @ExampleObject(
                    name = "UpdateRole",
                    summary = "Update member role to admin",
                    value = """
                    {
                        "role": "admin"
                    }
                    """
                )
            }
        )
    )
    @APIResponse(
        responseCode = "200",
        description = "Member role updated successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiResponse.class)
        )
    )
    @APIResponse(
        responseCode = "400",
        description = "Validation error atau cannot change owner role"
    )
    @APIResponse(
        responseCode = "403",
        description = "Forbidden - Only owner can update member role"
    )
    @APIResponse(
        responseCode = "404",
        description = "Member or organization not found"
    )
    public Response updateMemberRole(
            @PathParam("orgId") String orgId,
            @PathParam("memberId") String memberId,
            @Valid UpdateMemberRoleRequest request,
            @Context SecurityContext securityContext) {
        String userId = getUserIdFromSecurityContext(securityContext);

        User member = memberService.updateMemberRole(orgId, userId, memberId, request);

        return ResponseHelper.ok(member, "Member role updated successfully");
    }

    @DELETE
    @Path("/members/{memberId}")
    @RolesAllowed("USER")
    @SecurityRequirement(name = "Bearer")
    @Operation(
        summary = "Remove member from organization",
        description = """
            Remove member dari organization.
            
            ### Authorization
            Owner dan admin bisa remove member.
            
            ### Restrictions
            - Owner organization tidak bisa di-remove
            - Admin tidak bisa remove admin lain
            """
    )
    @APIResponse(
        responseCode = "200",
        description = "Member removed successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiResponse.class)
        )
    )
    @APIResponse(
        responseCode = "400",
        description = "Validation error atau cannot remove owner"
    )
    @APIResponse(
        responseCode = "403",
        description = "Forbidden - Only owner or admin can remove members"
    )
    @APIResponse(
        responseCode = "404",
        description = "Member or organization not found"
    )
    public Response removeMember(
            @PathParam("orgId") String orgId,
            @PathParam("memberId") String memberId,
            @Context SecurityContext securityContext) {
        String userId = getUserIdFromSecurityContext(securityContext);

        memberService.removeMember(orgId, userId, memberId);

        return ResponseHelper.ok(null, "Member removed successfully");
    }

    @POST
    @Path("/leave")
    @RolesAllowed("USER")
    @SecurityRequirement(name = "Bearer")
    @Operation(
        summary = "Leave organization",
        description = """
            Member meninggalkan organization.
            
            ### Restrictions
            - Owner tidak bisa leave organization
            - Member atau admin bisa leave
            """
    )
    @APIResponse(
        responseCode = "200",
        description = "Left organization successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiResponse.class)
        )
    )
    @APIResponse(
        responseCode = "400",
        description = "Owner cannot leave organization"
    )
    @APIResponse(
        responseCode = "403",
        description = "Forbidden - User is not a member"
    )
    @APIResponse(
        responseCode = "404",
        description = "Organization not found"
    )
    public Response leaveOrganization(
            @PathParam("orgId") String orgId,
            @Context SecurityContext securityContext) {
        String userId = getUserIdFromSecurityContext(securityContext);

        User user = memberService.leaveOrganization(orgId, userId);

        java.util.HashMap<String, Object> responseData = new java.util.HashMap<>();
        responseData.put("id", user.id.toHexString());
        responseData.put("organizationId", null);
        responseData.put("organizationRole", null);

        return ResponseHelper.ok(responseData, "Left organization successfully");
    }

    private String getUserIdFromSecurityContext(SecurityContext securityContext) {
        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            return null;
        }
        return securityContext.getUserPrincipal().getName();
    }
}
