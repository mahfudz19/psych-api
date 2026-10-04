package com.psycorp.psychapi.feature.user.api;

import java.util.List;

import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.mongodb.client.model.Filters;
import com.psycorp.psychapi.feature.auth.api.dto.request.SessionListRequest;
import com.psycorp.psychapi.feature.auth.api.dto.response.SessionResponse;
import com.psycorp.psychapi.feature.auth.service.AuthService;
import com.psycorp.psychapi.feature.subscription.api.dto.request.TransactionHistoryRequest;
import com.psycorp.psychapi.feature.subscription.api.dto.response.TransactionHistoryResponse;
import com.psycorp.psychapi.feature.subscription.models.Transaction;
import com.psycorp.psychapi.feature.subscription.service.TransactionService;
import com.psycorp.psychapi.feature.user.api.dto.request.UserListRequest;
import com.psycorp.psychapi.feature.user.api.dto.response.UserResponse;
import com.psycorp.psychapi.feature.user.models.User;
import com.psycorp.psychapi.feature.user.service.UserService;
import com.psycorp.psychapi.shared.response.ApiErrorResponse;
import com.psycorp.psychapi.shared.response.ApiResponse;
import com.psycorp.psychapi.shared.response.PaginationMeta;
import com.psycorp.psychapi.shared.response.ResponseHelper;
import com.psycorp.psychapi.shared.util.MongoFilter;

import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/users")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Users", description = "API untuk mengelola users")
public class UserResource {

    @Inject
    UserService userService;

    @Inject
    AuthService authService;

    @Inject
    TransactionService transactionService;

    @GET
    @RolesAllowed("SUPERADMIN")
    @Operation(summary = "Get all users with pagination, search, and filter", description = UserListRequest.DESCRIPTION)
    @APIResponse(responseCode = "200", description = "Users retrieved successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid request parameters", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "403", description = "Forbidden - Requires SUPERADMIN role", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response getAllUsers(@BeanParam UserListRequest request) {
        Bson filter = MongoFilter.fromRequest(request, UserListRequest.SEARCH_FIELDS);
        Bson sort   = MongoFilter.sort(request);

        PanacheQuery<User> users = userService
            .find(filter, sort)
            .page(request.page() - 1, request.limit());
        long total = userService.count(filter);
        
        List<UserResponse> data = users.stream().map(UserResponse::fromEntity).toList();
        PaginationMeta meta = PaginationMeta.of(request, total);
        
        return ResponseHelper.ok(data, "Users retrieved successfully", meta);
    }

    @GET
    @Path("/{id}/detail")
    @RolesAllowed("SUPERADMIN")
    @Operation(summary = "Get a user by ID", description = "Mengambil informasi detail user berdasarkan ObjectId.")
    @APIResponse(responseCode = "200", description = "User retrieved successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid ID format", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "403", description = "Forbidden - Requires SUPERADMIN role", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response getUserById(
        @Parameter(description = "User ObjectId", required = true, example = "507f1f77bcf86cd799439011")
        @PathParam("id") ObjectId id
    ) {
        User user = userService.findById(id);
        UserResponse data = UserResponse.fromEntity(user);
        return ResponseHelper.ok(data, "User retrieved successfully");
    }
    
    @GET
    @Path("/{id}/sessions")
    @RolesAllowed("SUPERADMIN")
    @Operation(summary = "Get user sessions by ID (SUPERADMIN)", description = "Mengambil daftar semua sesi login dari user tertentu berdasarkan userId dengan pagination.")
    @APIResponse(responseCode = "200", description = "Sessions retrieved successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "403", description = "Forbidden - Requires SUPERADMIN role", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response getSessionsByUserId(
        @Parameter(description = "User ObjectId", required = true)
        @PathParam("id") org.bson.types.ObjectId userId,
        @BeanParam SessionListRequest request
    ) {
        Bson filter = MongoFilter.fromRequest(request, SessionListRequest.SESSION_SEARCH_FIELDS);
        Bson sort = MongoFilter.sort(request);

        List<SessionResponse> sessions = authService.getSessions(userId, filter, sort, request.page(), request.limit());
        long total = authService.getSessionsCount(userId, filter);

        PaginationMeta meta = PaginationMeta.of(request, total);
        return ResponseHelper.ok(sessions, "Sessions retrieved successfully", meta);
    }

    @GET
    @Path("/{id}/transactions")
    @RolesAllowed("SUPERADMIN")
    @Operation(summary = "Get transactions by user ID (SUPERADMIN)", description = "Melihat riwayat transaksi user tertentu berdasarkan userId")
    public Response getTransactionsByUser(
        @Parameter(description = "User/Subscriber ObjectId", required = true)
        @PathParam("id") ObjectId userId,
        @BeanParam TransactionHistoryRequest request
    ) {
        Bson baseFilter = Filters.eq("subscriberId", userId);
        Bson searchFilter = MongoFilter.fromRequest(request, TransactionHistoryRequest.SEARCH_FIELDS);
        Bson finalFilter = MongoFilter.and(baseFilter, searchFilter);
        Bson sort = MongoFilter.sort(request);

        PanacheQuery<Transaction> query = transactionService.getTransactionHistory(finalFilter, sort, request.page(), request.limit());
        long total = transactionService.count(finalFilter);
        List<Transaction> transactions = query.list();

        List<TransactionHistoryResponse> data = transactionService.buildHistoryResponse(transactions);
        PaginationMeta meta = PaginationMeta.of(request, total);

        return ResponseHelper.ok(data, "Transaksi user berhasil diambil", meta);
    }
}