package com.psycorp.psychapi.feature.organization.api;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import static com.psycorp.psychapi.feature.organization.api.dto.request.OrganizationMemberRequests.LEAVE_ORGANIZATION_DESCRIPTION;
import static com.psycorp.psychapi.feature.organization.api.dto.request.OrganizationMemberRequests.MEMBERS_LIST_DESCRIPTION;
import static com.psycorp.psychapi.feature.organization.api.dto.request.OrganizationMemberRequests.MEMBER_DETAIL_DESCRIPTION;
import com.psycorp.psychapi.feature.organization.api.dto.request.OrganizationMemberRequests.MembersListRequest;
import static com.psycorp.psychapi.feature.organization.api.dto.request.OrganizationMemberRequests.REMOVE_MEMBER_DESCRIPTION;
import com.psycorp.psychapi.feature.organization.api.dto.request.OrganizationMemberRequests.UpdateMemberRoleRequest;
import com.psycorp.psychapi.feature.organization.api.dto.response.OrganizationMemberResponse;
import com.psycorp.psychapi.feature.organization.service.OrganizationMemberService;
import com.psycorp.psychapi.feature.user.model.User;
import com.psycorp.psychapi.shared.response.PaginationMeta;
import com.psycorp.psychapi.shared.response.ResponseHelper;

import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/organizations/{orgId}")
@Authenticated
@RolesAllowed("ORGANIZATION")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Organization Members", description = "API untuk mengelola members organization")
public class OrganizationMemberResource {

    @Inject
    OrganizationMemberService memberService;

    @GET
    @Path("/members")
    @Operation(summary = "Get all organization members", description = MEMBERS_LIST_DESCRIPTION)
    @APIResponse(responseCode = "200", description = "Members retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrganizationMemberResponse.class)))
    @APIResponse(responseCode = "403", description = "Forbidden - Only owner or admin can view all members")
    @APIResponse(responseCode = "404", description = "Organization not found")
    public Response getMembers(
            @PathParam("orgId") String orgId,
            @BeanParam @Valid MembersListRequest request,
            @Context ContainerRequestContext requestContext
        ) {
        User currentUser = (User) requestContext.getProperty("validatedUser");

        List<User> members = memberService.getMembersByOrganizationId(orgId, currentUser, request);
        long total = memberService.getMembersCount(orgId, request.search(), request.filter());
        
        List<OrganizationMemberResponse> responses = members.stream()
            .map(OrganizationMemberResponse::fromEntity)
            .toList();
        
        PaginationMeta meta = PaginationMeta.of(request.page(), request.limit(), total);

        return ResponseHelper.ok(responses, "Members retrieved successfully", meta);
    }

    @GET
    @Path("/members/{memberId}")
    @Operation(summary = "Get member detail", description = MEMBER_DETAIL_DESCRIPTION)
    @APIResponse(responseCode = "200", description = "Member retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrganizationMemberResponse.class)))
    @APIResponse(responseCode = "403", description = "Forbidden - User is not a member of this organization")
    @APIResponse(responseCode = "404", description = "Member or organization not found")
    public Response getMemberById(
            @PathParam("orgId") String orgId,
            @PathParam("memberId") String memberId,
            @Context ContainerRequestContext requestContext
        ) {
        User currentUser = (User) requestContext.getProperty("validatedUser");

        User member = memberService.getMemberById(orgId, memberId, currentUser);
        OrganizationMemberResponse response = OrganizationMemberResponse.fromEntity(member);

        return ResponseHelper.ok(response, "Member retrieved successfully");
    }

    @PATCH
    @Path("/members/{memberId}/role")
    @Operation(summary = "Update member role", description = UpdateMemberRoleRequest.DESCRIPTION)
    @APIResponse(responseCode = "200", description = "Member role updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrganizationMemberResponse.class)))
    @APIResponse(responseCode = "400", description = "Validation error atau cannot change owner role")
    @APIResponse(responseCode = "403", description = "Forbidden - Only owner can update member role")
    @APIResponse(responseCode = "404", description = "Member or organization not found")
    public Response updateMemberRole(
            @PathParam("orgId") String orgId,
            @PathParam("memberId") String memberId,
            @Valid UpdateMemberRoleRequest request,
            @Context ContainerRequestContext requestContext
        ) {
        User currentUser = (User) requestContext.getProperty("validatedUser");

        User member = memberService.updateMemberRole(orgId, currentUser, memberId, request);
        OrganizationMemberResponse response = OrganizationMemberResponse.fromEntity(member);

        return ResponseHelper.ok(response, "Member role updated successfully");
    }

    @DELETE
    @Path("/members/{memberId}")
    @Operation(summary = "Remove member from organization", description = REMOVE_MEMBER_DESCRIPTION)
    @APIResponse(responseCode = "200", description = "Member removed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrganizationMemberResponse.class)))
    @APIResponse(responseCode = "400", description = "Validation error atau cannot remove owner")
    @APIResponse(responseCode = "403", description = "Forbidden - Only owner or admin can remove members")
    @APIResponse(responseCode = "404", description = "Member or organization not found")
    public Response removeMember(
            @PathParam("orgId") String orgId,
            @PathParam("memberId") String memberId,
            @Context ContainerRequestContext requestContext
        ) {
        User currentUser = (User) requestContext.getProperty("validatedUser");

        memberService.removeMember(orgId, currentUser, memberId);

        return ResponseHelper.success("Member removed successfully");
    }

    @PATCH
    @Path("/leave")
    @Operation(summary = "Leave organization", description = LEAVE_ORGANIZATION_DESCRIPTION)
    @APIResponse(responseCode = "200", description = "Left organization successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrganizationMemberResponse.class)))
    @APIResponse(responseCode = "400", description = "Owner cannot leave organization")
    @APIResponse(responseCode = "403", description = "Forbidden - User is not a member")
    @APIResponse(responseCode = "404", description = "Organization not found")
    public Response leaveOrganization(
            @PathParam("orgId") String orgId,
            @Context ContainerRequestContext requestContext
        ) {
        User currentUser = (User) requestContext.getProperty("validatedUser");

        User user = memberService.leaveOrganization(orgId, currentUser);
        OrganizationMemberResponse response = OrganizationMemberResponse.fromEntity(user);

        return ResponseHelper.ok(response, "Left organization successfully");
    }
}
