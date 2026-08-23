package com.psycorp.psychapi.feature.user.api;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.psycorp.psychapi.feature.user.api.dto.request.CreateUserRequest;
import com.psycorp.psychapi.feature.user.api.dto.request.UpdateUserRequest;
import com.psycorp.psychapi.feature.user.api.dto.request.UserListRequest;
import com.psycorp.psychapi.feature.user.api.dto.response.UserResponse;
import com.psycorp.psychapi.feature.user.model.User;
import com.psycorp.psychapi.feature.user.service.UserService;
import com.psycorp.psychapi.shared.response.PaginationMeta;
import com.psycorp.psychapi.shared.response.ResponseHelper;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Users", description = "API untuk mengelola users")
public class UserResource {

    @Inject
    UserService userService;

    @GET
    @Operation(summary = "Get all users with pagination, search, and filter")
    @APIResponse(responseCode = "200", description = "Successful response")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    public Response getAllUsers(@BeanParam UserListRequest request) {
        List<User> users = userService.getAllUsers(
            request.search(), 
            request.filter(), 
            request.sortBy(), 
            request.sortOrder(), 
            request.page(), 
            request.limit()
        );
        long total = userService.getTotalUsersCount(request.search(), request.filter());
        
        List<UserResponse> data = users.stream().map(UserResponse::fromEntity).toList();
        PaginationMeta meta = PaginationMeta.of(request.page(), request.limit(), total);
        
        return ResponseHelper.ok(data, "Users retrieved successfully", meta);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get a user by ID")
    @APIResponse(responseCode = "200", description = "Successful response")
    @APIResponse(responseCode = "404", description = "User not found")
    public Response getUserById(@PathParam("id") String id) {
        User user = userService.getUserById(id);
        UserResponse data = UserResponse.fromEntity(user);
        return ResponseHelper.ok(data, "User retrieved successfully");
    }

    @POST
    @Operation(summary = "Create a new user")
    @RequestBody(
        description = "Create user request",
        required = true,
        content = @Content(schema = @Schema(implementation = CreateUserRequest.class))
    )
    @APIResponse(responseCode = "201", description = "User created successfully")
    @APIResponse(responseCode = "409", description = "Email already exists")
    @APIResponse(responseCode = "422", description = "Validation error")
    public Response createUser(@Valid CreateUserRequest request) {
        User user = userService.createUser(
            request.email(), 
            request.password(), 
            request.fullName(), 
            request.phone(), 
            request.bio(), 
            request.referredBy()
        );
        UserResponse data = UserResponse.fromEntity(user);
        return ResponseHelper.created(data, "User created successfully");
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update user profile")
    @APIResponse(responseCode = "200", description = "User updated successfully")
    @APIResponse(responseCode = "404", description = "User not found")
    public Response updateUser(@PathParam("id") String id, @Valid UpdateUserRequest request) {
        User user = userService.updateUser(
            id,
            request.email(),
            request.fullName(),
            request.phone(),
            request.bio(),
            request.status()
        );
        UserResponse data = UserResponse.fromEntity(user);
        return ResponseHelper.ok(data, "User updated successfully");
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete a user permanently")
    @APIResponse(responseCode = "200", description = "User deleted successfully")
    @APIResponse(responseCode = "404", description = "User not found")
    public Response deleteUser(@PathParam("id") String id) {
        userService.deleteUser(id);
        return ResponseHelper.success("User deleted successfully");
    }

    @DELETE
    @Path("/{id}/soft")
    @Operation(summary = "Soft delete a user account")
    @APIResponse(responseCode = "200", description = "User soft deleted successfully")
    @APIResponse(responseCode = "404", description = "User not found")
    public Response softDeleteUser(@PathParam("id") String id) {
        User user = userService.softDeleteUser(id);
        UserResponse data = UserResponse.fromEntity(user);
        return ResponseHelper.ok(data, "User soft deleted successfully");
    }
}
