package com.psycorp.psychapi.api.dto;

import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

public final class UserRequests {
    
    private UserRequests() {}

    public record CreateUserRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,
        
        @NotBlank(message = "Password is required")
        String password,
        
        @NotBlank(message = "Full name is required")
        String fullName,
        
        String phone,
        String bio,
        String referredBy
    ) {}

    public record UpdateUserRequest(
        @Email(message = "Invalid email format")
        String email,
        
        String fullName,
        String phone,
        String bio,
        
        @Pattern(regexp = "^(active|inactive|suspended|deleted)$", message = "Status must be one of: active, inactive, suspended, deleted")
        String status
    ) {}

    public record UpdateUserWithMapRequest(
        Map<String, Object> fields
    ) {}

    public record UserListRequest(
        @QueryParam("search") 
        @Parameter(
            description = "Search keyword to find in email, fullName, phone, and bio fields (case-insensitive)", 
            required = false
        ) 
        String search,

        @QueryParam("filter") 
        @Parameter(
            description = "Filter with format 'field:operator:value'. Supported operators: in, nin, eq, ne, gt, gte, lt, lte, contains. Example: 'status:in:active,suspended' or 'createdAt:gte:2024-01-01'", 
            required = false
        ) 
        String filter,
        
        @QueryParam("page") 
        @DefaultValue("1")
        @Parameter(description = "Page number (1-based)", example = "1") 
        int page,
        
        @QueryParam("limit") 
        @DefaultValue("10")
        @Parameter(description = "Items per page", example = "10") 
        int limit,
        
        @QueryParam("sortBy") 
        @DefaultValue("createdAt")
        @Parameter(description = "Sort field", example = "createdAt") 
        String sortBy,
        
        @QueryParam("sortOrder") 
        @DefaultValue("desc")
        @Parameter(description = "Sort order: asc or desc", example = "desc") 
        String sortOrder
    ) {}
}
