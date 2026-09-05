package com.psycorp.psychapi.feature.organization.api;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.psycorp.psychapi.feature.auth.api.dto.response.UserInfoResponse;
import com.psycorp.psychapi.feature.organization.api.dto.request.CreateOrganizationRequest;
import com.psycorp.psychapi.feature.organization.api.dto.request.DeleteOrganizationRequest;
import com.psycorp.psychapi.feature.organization.api.dto.request.OrganizationListRequest;
import com.psycorp.psychapi.feature.organization.api.dto.request.UpdateOrganizationRequest;
import com.psycorp.psychapi.feature.organization.api.dto.response.OrganizationResponse;
import com.psycorp.psychapi.feature.organization.api.dto.response.OrganizationWithOwnerResponse;
import com.psycorp.psychapi.feature.organization.model.Organization;
import com.psycorp.psychapi.feature.organization.service.OrganizationService;
import com.psycorp.psychapi.feature.user.model.User;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;
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
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/organizations")
@Authenticated
@RolesAllowed("ORGANIZATION")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Organizations", description = "API untuk mengelola organizations")
public class OrganizationResource {

    @Inject
    OrganizationService organizationService;

    @POST
    @SecurityRequirement(name = "Bearer")
    @Operation(summary = "Create organization baru")
    @RequestBody(description = "Create organization request", required = true, content = @Content(schema = @Schema(implementation = CreateOrganizationRequest.class)))
    @APIResponse(responseCode = "201", description = "Organization created successfully")
    @APIResponse(responseCode = "400", description = "Validation error")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    public Response createOrganization(@Valid CreateOrganizationRequest request, @Context ContainerRequestContext requestContext) {
        User user = (User) requestContext.getProperty("validatedUser");
        if (user == null) throw new ValidationException("USER_NOT_FOUND", "User not found");
        
        Organization organization = organizationService.createOrganization(user, request);
        User owner = organizationService.getOrganizationOwner(organization);

        OrganizationWithOwnerResponse data = OrganizationWithOwnerResponse.of(organization, owner);
        return ResponseHelper.created(data, "Organization created successfully");
    }

    @GET
    @SecurityRequirement(name = "Bearer")
    @Operation(summary = "List organizations user")
    @APIResponse(responseCode = "200", description = "Organizations retrieved successfully")
    public Response getOrganizations(@BeanParam OrganizationListRequest request, @Context ContainerRequestContext requestContext) {
        User user = (User) requestContext.getProperty("validatedUser");
        
        if (user == null) {
            throw new ValidationException("USER_NOT_FOUND", "User not found");
        }

        List<Organization> organizations = organizationService.getOrganizations(
            user, request.page(), request.limit(), request.sortBy(), request.sortOrder()
        );
        long total = organizationService.getOrganizationsCount(user);
        
        List<OrganizationResponse> data = organizations.stream()
            .map(OrganizationResponse::fromEntity)
            .toList();
        
        PaginationMeta meta = PaginationMeta.of(request, total);
        return ResponseHelper.ok(data, "Organizations retrieved successfully", meta);
    }

    @GET
    @SecurityRequirement(name = "Bearer")
    @Path("/{orgId}/detail")
    @Operation(summary = "Get organization detail")
    @APIResponse(responseCode = "200", description = "Organization retrieved successfully")
    @APIResponse(responseCode = "404", description = "Organization not found")
    public Response getOrganizationById(@PathParam("orgId") String orgId, @Context ContainerRequestContext requestContext) {
        User user = (User) requestContext.getProperty("validatedUser");
        
        if (user == null) {
            throw new ValidationException("USER_NOT_FOUND", "User not found");
        }

        Organization organization = organizationService.getOrganizationById(orgId, user);
        OrganizationResponse data = OrganizationResponse.fromEntity(organization);
        return ResponseHelper.ok(data, "Organization retrieved successfully");
    }

    @PATCH
    @SecurityRequirement(name = "Bearer")
    @Path("/{orgId}/update")
    @Operation(summary = "Update organization info")
    @RequestBody(description = "Update organization request", required = true, content = @Content(schema = @Schema(implementation = UpdateOrganizationRequest.class)))
    @APIResponse(responseCode = "200", description = "Organization updated successfully")
    @APIResponse(responseCode = "400", description = "Validation error")
    @APIResponse(responseCode = "403", description = "Forbidden")
    @APIResponse(responseCode = "404", description = "Organization not found")
    public Response updateOrganization(
            @PathParam("orgId") String orgId,
            @Valid UpdateOrganizationRequest request,
            @Context ContainerRequestContext requestContext
        ) {
        User user = (User) requestContext.getProperty("validatedUser");
        
        if (user == null) {
            throw new ValidationException("USER_NOT_FOUND", "User not found");
        }

        Organization organization = organizationService.updateOrganization(orgId, user, request);
        User owner = organizationService.getOrganizationOwner(organization);

        OrganizationWithOwnerResponse data = OrganizationWithOwnerResponse.of(organization, owner);
        return ResponseHelper.created(data, "Organization created successfully");
    }

    @DELETE
    @SecurityRequirement(name = "Bearer")
    @Path("/{orgId}/delete")
    @Operation(summary = "Soft delete organization")
    @RequestBody(description = "Delete organization request", required = true, content = @Content(schema = @Schema(implementation = DeleteOrganizationRequest.class)))
    @APIResponse(responseCode = "200", description = "Organization deleted successfully")
    @APIResponse(responseCode = "400", description = "Validation error")
    @APIResponse(responseCode = "403", description = "Forbidden")
    @APIResponse(responseCode = "404", description = "Organization not found")
    public Response deleteOrganization(
            @PathParam("orgId") String orgId,
            @Valid DeleteOrganizationRequest request,
            @Context ContainerRequestContext requestContext
        ) {
        User user = (User) requestContext.getProperty("validatedUser");
        
        if (user == null) {
            throw new ValidationException("USER_NOT_FOUND", "User not found");
        }

        organizationService.deleteOrganization(orgId, user, request.confirmation());
        return ResponseHelper.success("Organization deleted successfully");
    }

    @POST
    @SecurityRequirement(name = "Bearer")
    @Path("/invite-code")
    @Operation(summary = "Generate atau regenerate invite code untuk organization")
    @APIResponse(responseCode = "200", description = "Invite code generated successfully")
    @APIResponse(responseCode = "403", description = "User tidak punya organization atau bukan owner/admin")
    public Response generateInviteCode(@Context ContainerRequestContext requestContext) {
        User user = (User) requestContext.getProperty("validatedUser");
        if (user == null) throw new ValidationException("USER_NOT_FOUND", "User not found");

        organizationService.generateOrRegenerateInviteCode(user);
        return ResponseHelper.ok(UserInfoResponse.from(user), "Invite code generated successfully");
    }
}
