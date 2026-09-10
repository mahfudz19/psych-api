package com.psycorp.psychapi.feature.storage.api;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.psycorp.psychapi.feature.storage.api.dto.request.UploadUrlRequest;
import com.psycorp.psychapi.feature.storage.api.dto.response.UploadUrlResponse;
import com.psycorp.psychapi.feature.storage.service.StorageService;
import com.psycorp.psychapi.feature.user.model.User;
import com.psycorp.psychapi.shared.response.ApiErrorResponse;
import com.psycorp.psychapi.shared.response.ApiResponse;
import com.psycorp.psychapi.shared.response.ResponseHelper;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/files")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "File Storage", description = "API endpoints untuk upload dan manajemen file di GCS")
@Authenticated
public class FileResource {

    @Inject
    StorageService storageService;

    @POST
    @Path("/upload-url")
    @Operation(summary = "Request Signed URL untuk upload", description = UploadUrlRequest.DESCRIPTION)
    @APIResponse(responseCode = "200", description = "Signed URL generated", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @APIResponse(responseCode = "400", description = "Validation failed - missing or invalid fields", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "403", description = "Forbidden - User not found or inactive", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response getUploadUrl(
        @Valid UploadUrlRequest request,
        @Context ContainerRequestContext requestContext
    ) {
        User user = (User) requestContext.getProperty("validatedUser");
        if (user == null) throw new ForbiddenException("Authentication required");

        UploadUrlResponse response = storageService.generateUploadUrl(
            user.getId().toString(),
            request.filename(),
            request.mimeType(),
            request.category(),
            request.visibility()
        );

        return ResponseHelper.ok(response, "Upload URL generated");
    }
}