package com.psycorp.psychapi.feature.subscription.api;

import java.util.List;

import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.mongodb.client.model.Filters;
import com.psycorp.psychapi.feature.subscription.api.dto.request.CreateSubscriptionPlanRequest;
import com.psycorp.psychapi.feature.subscription.api.dto.request.SubscriptionPlanListRequest;
import com.psycorp.psychapi.feature.subscription.api.dto.request.UpdateSubscriptionPlanRequest;
import com.psycorp.psychapi.feature.subscription.api.dto.response.SubscriptionPlanResponse;
import com.psycorp.psychapi.feature.subscription.model.SubscriptionPlan;
import com.psycorp.psychapi.feature.subscription.service.SubscriptionPlanService;
import com.psycorp.psychapi.shared.response.ApiErrorResponse;
import com.psycorp.psychapi.shared.response.ApiResponse;
import com.psycorp.psychapi.shared.response.PaginationMeta;
import com.psycorp.psychapi.shared.response.ResponseHelper;
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
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/subscription-plans")
@Authenticated
@RolesAllowed("SUPERADMIN")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Subscription Plans", description = "API untuk mengelola katalog paket langganan")
@SecurityScheme(securitySchemeName = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT", description = "JWT Bearer token authentication")
public class SubscriptionPlanResource {

    @Inject
    SubscriptionPlanService planService;

    @POST
    @Operation(summary = "Create subscription plan baru")
    @RequestBody(description = "Data paket langganan", required = true, content = @Content(schema = @Schema(implementation = CreateSubscriptionPlanRequest.class)))
    @APIResponse(responseCode = "201", description = "Plan created successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @APIResponse(responseCode = "400", description = "Validation failed or code already exists", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "403", description = "Forbidden - Requires SUPERADMIN role", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response createPlan(@Valid CreateSubscriptionPlanRequest request) {
        SubscriptionPlan plan = planService.create(
            request.name(),
            request.code(),
            request.price(),
            request.durationDays(),
            request.targetAudience(),
            request.maxSeats()
        );
        return ResponseHelper.created(SubscriptionPlanResponse.fromEntity(plan), "Subscription plan created successfully");
    }

    @GET
    @Operation(summary = "Get all subscription plans with pagination", description = SubscriptionPlanListRequest.DESCRIPTION)
    @APIResponse(responseCode = "200", description = "Plans retrieved successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid request parameters", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "403", description = "Forbidden - Requires SUPERADMIN role", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response getPlans(@BeanParam SubscriptionPlanListRequest request) {
        Bson baseFilter = Filters.eq("deletedAt", null);
        Bson searchFilter = MongoFilter.fromRequest(request, SubscriptionPlanListRequest.SEARCH_FIELDS);
        Bson finalFilter = MongoFilter.and(baseFilter, searchFilter);
        Bson sort = MongoFilter.sort(request);

        PanacheQuery<SubscriptionPlan> query = planService.find(finalFilter, sort).page(request.page() - 1, request.limit());
        long total = planService.count(finalFilter);

        List<SubscriptionPlanResponse> data = query.stream().map(SubscriptionPlanResponse::fromEntity).toList();
        PaginationMeta meta = PaginationMeta.of(request, total);

        return ResponseHelper.ok(data, "Subscription plans retrieved successfully", meta);
    }

    @PATCH
    @Path("/{id}")
    @Operation(summary = "Update subscription plan")
    @APIResponse(responseCode = "200", description = "Plan updated successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @APIResponse(responseCode = "400", description = "Validation failed or code already exists", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "403", description = "Forbidden - Requires SUPERADMIN role", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "404", description = "Plan not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response updatePlan(@PathParam("id") String id, @Valid UpdateSubscriptionPlanRequest request) {
        SubscriptionPlan plan = planService.update(
            new ObjectId(id),
            request.name(),
            request.code(),
            request.price(),
            request.durationDays(),
            request.targetAudience(),
            request.maxSeats()
        );
        return ResponseHelper.ok(SubscriptionPlanResponse.fromEntity(plan), "Subscription plan updated successfully");
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Soft delete subscription plan")
    @APIResponse(responseCode = "200", description = "Plan deleted successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "403", description = "Forbidden - Requires SUPERADMIN role", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "404", description = "Plan not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response deletePlan(@PathParam("id") String id) {
        planService.softDelete(new ObjectId(id));
        return ResponseHelper.ok(null, "Subscription plan deleted successfully");
    }
}