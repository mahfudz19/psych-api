package com.psycorp.psychapi.feature.organization.api;

import java.util.List;

import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.mongodb.client.model.Filters;
import com.psycorp.psychapi.feature.auth.api.dto.response.UserInfoResponse;
import static com.psycorp.psychapi.feature.organization.api.dto.request.OrganizationMemberRequests.JOIN_ORGANIZATION_DESCRIPTION;
import static com.psycorp.psychapi.feature.organization.api.dto.request.OrganizationMemberRequests.LEAVE_ORGANIZATION_DESCRIPTION;
import static com.psycorp.psychapi.feature.organization.api.dto.request.OrganizationMemberRequests.MEMBERS_LIST_DESCRIPTION;
import static com.psycorp.psychapi.feature.organization.api.dto.request.OrganizationMemberRequests.MEMBER_DETAIL_DESCRIPTION;
import com.psycorp.psychapi.feature.organization.api.dto.request.OrganizationMemberRequests.MembersListRequest;
import static com.psycorp.psychapi.feature.organization.api.dto.request.OrganizationMemberRequests.REMOVE_MEMBER_DESCRIPTION;
import static com.psycorp.psychapi.feature.organization.api.dto.request.OrganizationMemberRequests.SEARCH_FIELDS;
import com.psycorp.psychapi.feature.organization.api.dto.request.OrganizationMemberRequests.UpdateMemberRoleRequest;
import com.psycorp.psychapi.feature.organization.api.dto.response.OrganizationMemberDetailResponse;
import com.psycorp.psychapi.feature.organization.api.dto.response.OrganizationMemberResponse;
import com.psycorp.psychapi.feature.organization.model.Organization;
import com.psycorp.psychapi.feature.organization.service.OrganizationMemberService;
import com.psycorp.psychapi.feature.organization.service.OrganizationService;
import com.psycorp.psychapi.feature.user.model.User;
import com.psycorp.psychapi.feature.user.model.User.OrganizationRole;
import com.psycorp.psychapi.feature.user.service.UserService;
import com.psycorp.psychapi.infrastructure.exception.NotFoundException;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;
import com.psycorp.psychapi.shared.response.PaginationMeta;
import com.psycorp.psychapi.shared.response.ResponseHelper;
import com.psycorp.psychapi.shared.util.DocumentUpdater;
import com.psycorp.psychapi.shared.util.MongoFilter;

import io.quarkus.mongodb.panache.PanacheQuery;
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

@Path("/api/v1/organizations/{orgId}/members")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Organization Members", description = "API untuk mengelola members organization")
public class OrganizationMemberResource {

    @Inject
    OrganizationMemberService memberService;

    @Inject
    UserService userservice;

    @Inject
    OrganizationService organizationService;

    @GET
    @RolesAllowed({"ORG_OWNER", "ORG_ADMIN"})
    @Operation(summary = "Get all organization members", description = MEMBERS_LIST_DESCRIPTION)
    @APIResponse(responseCode = "200", description = "Members retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrganizationMemberResponse.class)))
    @APIResponse(responseCode = "403", description = "Forbidden - Only owner or admin can view all members")
    @APIResponse(responseCode = "404", description = "Organization not found")
    public Response getMembers(@PathParam("orgId") ObjectId orgId, @BeanParam @Valid MembersListRequest request, @Context ContainerRequestContext requestContext) {
        User currentUser = (User) requestContext.getProperty("validatedUser");
        if (currentUser == null) throw new ValidationException("USER_NOT_FOUND", "User not found");

        Bson baseFilter = Filters.eq("organizationId", orgId);
        Bson requestFilter = MongoFilter.fromRequest(request, SEARCH_FIELDS);
        Bson finalFilter = MongoFilter.and(baseFilter, requestFilter);
        Bson sort = MongoFilter.sort(request);

        PanacheQuery<User> members = userservice
            .find(finalFilter, sort)
            .page(request.page() - 1, request.limit());
        long total = userservice.count(finalFilter);

        List<OrganizationMemberResponse> responses = members.stream().map(OrganizationMemberResponse::fromEntity).toList();
        PaginationMeta meta = PaginationMeta.of(request, total);

        return ResponseHelper.ok(responses, "Members retrieved successfully", meta);
    }

    @GET
    @Path("/{memberId}/detail")
    @RolesAllowed("ORGANIZATION")
    @Operation(summary = "Get member detail", description = MEMBER_DETAIL_DESCRIPTION)
    @APIResponse(responseCode = "200", description = "Member retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrganizationMemberResponse.class)))
    @APIResponse(responseCode = "403", description = "Forbidden - User is not a member of this organization")
    @APIResponse(responseCode = "404", description = "Member or organization not found")
    public Response getMemberById(@PathParam("orgId") ObjectId orgId, @PathParam("memberId") ObjectId memberId, @Context ContainerRequestContext requestContext) {
        User currentUser = (User) requestContext.getProperty("validatedUser");
        if (currentUser == null) throw new ValidationException("USER_NOT_FOUND", "User not found");

        User member = getMemberOrgById(orgId, memberId, currentUser);
        OrganizationMemberDetailResponse response = OrganizationMemberDetailResponse.fromEntity(member);

        return ResponseHelper.ok(response, "Member retrieved successfully");
    }

    @PATCH
    @Path("/{memberId}/role")
    @RolesAllowed({"ORG_OWNER", "ORG_ADMIN"})
    @Operation(summary = "Update member role", description = UpdateMemberRoleRequest.DESCRIPTION)
    @APIResponse(responseCode = "200", description = "Member role updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrganizationMemberResponse.class)))
    @APIResponse(responseCode = "400", description = "Validation error atau cannot change owner role")
    @APIResponse(responseCode = "403", description = "Forbidden - Only owner can update member role")
    @APIResponse(responseCode = "404", description = "Member or organization not found")
    public Response updateMemberRole(@PathParam("orgId") ObjectId orgId, @PathParam("memberId") ObjectId memberId, @Valid UpdateMemberRoleRequest request, @Context ContainerRequestContext requestContext) {
        User currentUser = (User) requestContext.getProperty("validatedUser");
        if (currentUser == null) throw new ValidationException("USER_NOT_FOUND", "User not found");

        User member = getMemberOrgById(orgId, memberId, currentUser);

        if (member.getId().equals(currentUser.getId())) 
            throw new ValidationException("CANNOT_CHANGE_YOUR_SELF", "Cannot change the role of your account");
        if (OrganizationRole.OWNER.equals(member.getOrganizationRole())) 
            throw new ValidationException("CANNOT_CHANGE_OWNER", "Cannot change the role of organization owner");

        DocumentUpdater updater = DocumentUpdater.update()
            .set("organizationRole", request.role());

        member.executeUpdate(updater.build());

        OrganizationMemberResponse response = OrganizationMemberResponse.fromEntity(member);
        return ResponseHelper.ok(response, "Member role updated successfully");
    }

    @DELETE
    @Path("/{memberId}/kick")
    @RolesAllowed({"ORG_OWNER", "ORG_ADMIN"})
    @Operation(summary = "Remove member from organization", description = REMOVE_MEMBER_DESCRIPTION)
    @APIResponse(responseCode = "200", description = "Member removed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrganizationMemberResponse.class)))
    @APIResponse(responseCode = "400", description = "Validation error atau cannot remove owner")
    @APIResponse(responseCode = "403", description = "Forbidden - Only owner or admin can remove members")
    @APIResponse(responseCode = "404", description = "Member or organization not found")
    public Response removeMember(@PathParam("orgId") ObjectId orgId, @PathParam("memberId") ObjectId memberId, @Context ContainerRequestContext requestContext) {
        User currentUser = (User) requestContext.getProperty("validatedUser");
        if (currentUser == null) throw new ValidationException("USER_NOT_FOUND", "User not found");

        User member = getMemberOrgById(orgId, memberId, currentUser);
        Organization organization = getOrganizationById(orgId);
        memberService.removeMember(organization, member, currentUser);

        return ResponseHelper.success("Member removed successfully");
    }

    @PATCH
    @Path("join")
    @Operation(summary = "Join organization", description = JOIN_ORGANIZATION_DESCRIPTION)
    @APIResponse(responseCode = "200", description = "Joined organization successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrganizationMemberResponse.class)))
    public Response joinOrganization(@PathParam("orgId") ObjectId orgId, @Context ContainerRequestContext requestContext) {
        User currentUser = (User) requestContext.getProperty("validatedUser");
        if (currentUser == null) throw new ValidationException("USER_NOT_FOUND", "User not found");
        
        Organization organization = getOrganizationById(orgId);

        User user = memberService.joinOrganization(organization, currentUser);

        UserInfoResponse response = UserInfoResponse.from(user);
        return ResponseHelper.ok(response, "Joined organization successfully");
    }


    @PATCH
    @Path("leave")
    @RolesAllowed("ORGANIZATION")
    @Operation(summary = "Leave organization", description = LEAVE_ORGANIZATION_DESCRIPTION)
    @APIResponse(responseCode = "200", description = "Left organization successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrganizationMemberResponse.class)))
    @APIResponse(responseCode = "400", description = "Owner cannot leave organization")
    @APIResponse(responseCode = "403", description = "Forbidden - User is not a member")
    @APIResponse(responseCode = "404", description = "Organization not found")
    public Response leaveOrganization(@PathParam("orgId") ObjectId orgId, @Context ContainerRequestContext requestContext) {
        User currentUser = (User) requestContext.getProperty("validatedUser");
        if (currentUser == null) throw new ValidationException("USER_NOT_FOUND", "User not found");

        Organization organization = getOrganizationById(orgId);
        
        User user = memberService.leaveOrganization(organization, currentUser);
        OrganizationMemberResponse response = OrganizationMemberResponse.fromEntity(user);

        return ResponseHelper.ok(response, "Left organization successfully");
    }

    private Organization getOrganizationById(ObjectId orgId) {
        Organization organization = organizationService.findById(orgId);
        if (organization == null) {
            throw new NotFoundException("ORGANIZATION_NOT_FOUND", "Organization with id " + orgId + " not found");
        }
        return organization;
    }

    private User getMemberOrgById(ObjectId orgId, ObjectId memberId, User currentUser) {
        if (currentUser.getOrganizationId() == null || !currentUser.getOrganizationId().equals(orgId)) {
            throw new ValidationException("UNAUTHORIZED", "User does not have access to this organization");
        }

        User user = userservice.findById(memberId);
        if (user == null) {
            throw new NotFoundException("USER_NOT_FOUND", "User with id " + memberId + " not found");
        }

        if (user.getOrganizationId() == null || !user.getOrganizationId().equals(orgId)) {
            throw new ValidationException("UNAUTHORIZED", "User is not a member of this organization");
        }

        return user;
    }
}
