package com.psycorp.psychapi.feature.referral.api;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.psycorp.psychapi.feature.referral.api.dto.request.RegenerateReferralRequest;
import com.psycorp.psychapi.feature.referral.api.dto.response.ReferralCodeResponse;
import com.psycorp.psychapi.feature.referral.api.dto.response.ReferralStatsResponse;
import com.psycorp.psychapi.feature.referral.service.ReferralService;
import com.psycorp.psychapi.feature.user.model.User;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;
import com.psycorp.psychapi.shared.response.ApiErrorResponse;
import com.psycorp.psychapi.shared.response.ApiResponse;
import com.psycorp.psychapi.shared.response.ResponseHelper;

import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/referral")
@Authenticated
@RolesAllowed("USER")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Referral Management", description = "API untuk mengelola referral codes")
@SecurityScheme(securitySchemeName = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT", description = "JWT Bearer token authentication")
public class ReferralResource {
    
    @Inject
    ReferralService referralService;
    
    @POST
    @Path("/regenerate")
    @Operation(summary = "Regenerate referral code", description = RegenerateReferralRequest.DESCRIPTION)
    @APIResponse(responseCode = "200", description = "Referral code regenerated successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid reason or validation error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "403", description = "Forbidden - Requires USER role", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "429", description = "Too many requests - Rate limit exceeded", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response regenerateReferralCode(@Valid RegenerateReferralRequest request, @Context ContainerRequestContext requestContext) {
        User user = (User) requestContext.getProperty("validatedUser");
        
        if (user == null) {
            throw new ValidationException("USER_NOT_FOUND", "User not found");
        }
        
        // Regenerate code
        String newCode = referralService.regenerateReferralCode(user, request.reason());
        
        // Build response using DTO
        String oldCode = user.getReferralCode() != null ? referralService.maskCode(user.getReferralCode()) : null;
        ReferralCodeResponse response = ReferralCodeResponse.ofRegenerate(newCode, oldCode, request.reason());
        
        return ResponseHelper.ok(response, "Referral code regenerated successfully");
    }

    @GET
    @Path("/stats")
    @Operation(summary = "Get referral statistics", description = ReferralStatsResponse.DESCRIPTION)
    @APIResponse(responseCode = "200", description = "Referral statistics retrieved successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "403", description = "Forbidden - Requires USER role", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response getReferralStats(@Context ContainerRequestContext requestContext) {
        User user = (User) requestContext.getProperty("validatedUser");
        
        if (user == null) {
            throw new ForbiddenException("Authentication required");
        }
        
        // Build response using DTO
        ReferralStatsResponse response = ReferralStatsResponse.fromEntity(user);
        
        return ResponseHelper.ok(response, "Referral statistics retrieved successfully");
    }
}