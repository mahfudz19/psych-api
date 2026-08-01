package com.psycorp.psychapi.api.resource;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.psycorp.psychapi.api.dto.OrganizationRequests.CreateOrganizationRequest;
import com.psycorp.psychapi.api.dto.OrganizationRequests.DeleteOrganizationRequest;
import com.psycorp.psychapi.api.dto.OrganizationRequests.OrganizationListRequest;
import com.psycorp.psychapi.api.dto.OrganizationRequests.UpdateOrganizationRequest;
import com.psycorp.psychapi.common.helper.ResponseHelper;
import com.psycorp.psychapi.common.response.ApiResponse;
import com.psycorp.psychapi.common.response.PaginationMeta;
import com.psycorp.psychapi.domain.model.Organization;
import com.psycorp.psychapi.domain.model.User;
import com.psycorp.psychapi.domain.service.OrganizationService;

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

@Path("/api/v1/organizations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Organizations", description = "API untuk mengelola organizations")
public class OrganizationResource {

    @Inject
    OrganizationService organizationService;

    @POST
    @RolesAllowed("USER")
    @SecurityRequirement(name = "Bearer")
    @Operation(
        summary = "Create organization baru",
        description = """
            Membuat organization baru untuk user yang sedang login.
            
            ### Authentication Required
            Endpoint ini memerlukan JWT token yang valid di cookie `COOKIE_NAME` atau Authorization header.
            
            ### Request Requirements
            - **name**: Nama organization (required)
            - **description**: Deskripsi organization (optional)
            - **website**: Website URL (optional)
            - **phone**: Nomor telepon (optional)
            - **email**: Email kontak (optional)
            - **address**: Alamat (optional)
            
            ### Response
            - Organization data yang sudah dibuat
            - User data yang sudah ter-update dengan organization info
            """
    )
    @RequestBody(
        description = "Create organization request",
        required = true,
        content = @Content(
            schema = @Schema(implementation = CreateOrganizationRequest.class),
            examples = {
                @ExampleObject(
                    name = "CreateOrganization",
                    summary = "Create new organization",
                    value = """
                    {
                        "name": "PT Company Name",
                        "description": "Leading provider of solutions",
                        "website": "https://company.com",
                        "phone": "+622112345678",
                        "email": "contact@company.com",
                        "address": "Jl. Sudirman No. 1, Jakarta"
                    }
                    """
                )
            }
        )
    )
    @APIResponse(
        responseCode = "201",
        description = "Organization created successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiResponse.class),
            examples = {
                @ExampleObject(
                    name = "OrganizationCreated",
                    summary = "Organization created successfully",
                    value = """
                    {
                        "success": true,
                        "message": "Organization created successfully",
                        "data": {
                            "organization": {
                                "id": "org_456",
                                "name": "PT Company Name",
                                "description": "Leading provider of solutions",
                                "website": "https://company.com",
                                "address": "Jl. Sudirman No. 1, Jakarta",
                                "phone": "+622112345678",
                                "email": "contact@company.com",
                                "ownerId": "usr_123",
                                "plan": "free_trial",
                                "status": true,
                                "trialStartsAt": "2026-07-24T18:00:00Z",
                                "trialEndsAt": "2026-08-07T18:00:00Z",
                                "seats": -1,
                                "seatsUsed": 1,
                                "createdAt": "2026-07-24T18:00:00Z",
                                "updatedAt": "2026-07-24T18:00:00Z"
                            },
                            "user": {
                                "id": "usr_123",
                                "organizationId": "org_456",
                                "organizationRole": "owner",
                                "organizationName": "PT Company Name",
                                "roles": ["USER", "ORGANIZATION"]
                            }
                        }
                    }
                    """
                )
            }
        )
    )
    @APIResponse(
        responseCode = "400",
        description = "Validation error - Invalid input"
    )
    @APIResponse(
        responseCode = "401",
        description = "Unauthorized - Missing or invalid JWT token"
    )
    public Response createOrganization(
            @Valid CreateOrganizationRequest request,
            @Context SecurityContext securityContext) {
        String userId = getUserIdFromSecurityContext(securityContext);

        Organization organization = organizationService.createOrganization(userId, request);

        // Build response data dengan organization dan user info
        Map<String, Object> responseData = buildCreateResponse(organization, userId);

        return ResponseHelper.created(responseData, "Organization created successfully");
    }

    @GET
    @RolesAllowed("USER")
    @SecurityRequirement(name = "Bearer")
    @Operation(
        summary = "List organizations user",
        description = """
            Mengambil daftar organization yang dimiliki oleh user yang sedang login.
            
            ### Authentication Required
            Endpoint ini memerlukan JWT token yang valid di cookie `COOKIE_NAME` atau Authorization header.
            
            ### Query Parameters
            - **page**: Page number (default: 1)
            - **limit**: Items per page (default: 10)
            - **sortBy**: Sort field (default: createdAt)
            - **sortOrder**: Sort order (default: desc)
            """
    )
    @APIResponse(
        responseCode = "200",
        description = "Organizations retrieved successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiResponse.class)
        )
    )
    public Response getOrganizations(
            @BeanParam OrganizationListRequest request,
            @Context SecurityContext securityContext) {
        String userId = getUserIdFromSecurityContext(securityContext);

        List<Organization> organizations = organizationService.getOrganizationsByUserId(
            userId,
            request.page(),
            request.limit(),
            request.sortBy(),
            request.sortOrder()
        );

        long total = organizationService.getOrganizationsCountByUserId(userId);
        int totalPages = (int) Math.ceil((double) total / request.limit());

        PaginationMeta meta = new PaginationMeta(
            request.page(),
            request.limit(),
            total,
            totalPages
        );

        return ResponseHelper.ok(organizations, "Organizations retrieved successfully", meta);
    }

    @GET
    @RolesAllowed("USER")
    @SecurityRequirement(name = "Bearer")
    @Path("/{orgId}")
    @Operation(
        summary = "Get organization detail",
        description = """
            Mengambil detail organization berdasarkan ID.
            
            ### Authentication Required
            Endpoint ini memerlukan JWT token yang valid.
            
            ### Authorization
            User harus memiliki akses ke organization (owner, admin, atau member).
            """
    )
    @APIResponse(
        responseCode = "200",
        description = "Organization retrieved successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiResponse.class)
        )
    )
    @APIResponse(
        responseCode = "404",
        description = "Organization not found"
    )
    public Response getOrganizationById(
            @PathParam("orgId") String orgId,
            @Context SecurityContext securityContext) {

        Organization organization = organizationService.getOrganizationById(orgId);

        return ResponseHelper.ok(organization, "Organization retrieved successfully");
    }

    @PATCH
    @RolesAllowed("USER")
    @SecurityRequirement(name = "Bearer")
    @Path("/{orgId}")
    @Operation(
        summary = "Update organization info",
        description = """
            Update informasi organization.
            
            ### Authentication Required
            Endpoint ini memerlukan JWT token yang valid.
            
            ### Authorization
            Hanya owner atau admin organization yang bisa update.
            
            ### Request
            Semua field optional. Hanya field yang diisi yang akan diupdate.
            """
    )
    @RequestBody(
        description = "Update organization request",
        required = true,
        content = @Content(
            schema = @Schema(implementation = UpdateOrganizationRequest.class),
            examples = {
                @ExampleObject(
                    name = "UpdateOrganization",
                    summary = "Update organization info",
                    value = """
                    {
                        "name": "PT Updated Name",
                        "description": "Updated description",
                        "website": "https://newwebsite.com",
                        "phone": "+622198765432",
                        "address": "New address"
                    }
                    """
                )
            }
        )
    )
    @APIResponse(
        responseCode = "200",
        description = "Organization updated successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiResponse.class)
        )
    )
    @APIResponse(
        responseCode = "400",
        description = "Validation error"
    )
    @APIResponse(
        responseCode = "403",
        description = "Forbidden - User doesn't have permission"
    )
    @APIResponse(
        responseCode = "404",
        description = "Organization not found"
    )
    public Response updateOrganization(
            @PathParam("orgId") String orgId,
            @Valid UpdateOrganizationRequest request,
            @Context SecurityContext securityContext) {
        String userId = getUserIdFromSecurityContext(securityContext);

        Organization organization = organizationService.updateOrganization(orgId, userId, request);

        return ResponseHelper.ok(organization, "Organization updated successfully");
    }

    @DELETE
    @RolesAllowed("USER")
    @SecurityRequirement(name = "Bearer")
    @Path("/{orgId}")
    @Operation(
        summary = "Soft delete organization",
        description = """
            Menghapus organization secara soft delete.
            
            ### Authentication Required
            Endpoint ini memerlukan JWT token yang valid.
            
            ### Authorization
            Hanya owner organization yang bisa delete.
            
            ### Requirements
            - Organization tidak boleh memiliki member lain (selain owner)
            - Harus menyertakan confirmation text: `DELETE_MY_ORGANIZATION`
            """
    )
    @RequestBody(
        description = "Delete organization request",
        required = true,
        content = @Content(
            schema = @Schema(implementation = DeleteOrganizationRequest.class),
            examples = {
                @ExampleObject(
                    name = "DeleteOrganization",
                    summary = "Delete organization with confirmation",
                    value = """
                    {
                        "confirmation": "DELETE_MY_ORGANIZATION"
                    }
                    """
                )
            }
        )
    )
    @APIResponse(
        responseCode = "200",
        description = "Organization deleted successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiResponse.class)
        )
    )
    @APIResponse(
        responseCode = "400",
        description = "Validation error atau organization masih memiliki member"
    )
    @APIResponse(
        responseCode = "403",
        description = "Forbidden - Only owner can delete organization"
    )
    @APIResponse(
        responseCode = "404",
        description = "Organization not found"
    )
    public Response deleteOrganization(
            @PathParam("orgId") String orgId,
            @Valid DeleteOrganizationRequest request,
            @Context SecurityContext securityContext) {
        String userId = getUserIdFromSecurityContext(securityContext);

        organizationService.deleteOrganization(orgId, userId, request.confirmation());

        return ResponseHelper.ok(null, "Organization deleted successfully");
    }

    private String getUserIdFromSecurityContext(SecurityContext securityContext) {
        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            return null;
        }
        return securityContext.getUserPrincipal().getName();
    }

    private Map<String, Object> buildCreateResponse(Organization organization, String userId) {
        User user = organizationService.getOrganizationOwner(organization);

        Map<String, Object> userInfo = Map.of(
            "id", userId,
            "organizationId", user.getOrganizationId() != null ? user.getOrganizationId().toHexString() : null,
            "organizationRole", user.getOrganizationRole(),
            "organizationName", user.getOrganizationName(),
            "roles", user.getRoles()
        );

        return Map.of(
            "organization", organization,
            "user", userInfo
        );
    }
}
