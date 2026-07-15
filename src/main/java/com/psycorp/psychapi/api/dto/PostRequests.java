package com.psycorp.psychapi.api.dto;

import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

public final class PostRequests {
    private PostRequests() {}

    public record CreatePostRequest(
        @NotBlank String title,
        String content,
        @Pattern(regexp = "^(draft|published)$") String status
    ) {}

    public record UpdatePostRequest(
        String title,
        String content,
        @Pattern(regexp = "^(draft|published)$") String status
    ) {}

    public record PostListRequest(
        @QueryParam("search") 
        @Parameter(
            description = "Search keyword to find in title and content fields (case-insensitive)", 
            required = false
        ) 
        String search,

        @QueryParam("filter") 
        @Parameter(
            description = "Filter with format 'field:operator:value'. Supported operators: in, nin, eq, ne, gt, gte, lt, lte, contains. Example: 'status:in:draft,published' or 'createdAt:gte:2024-01-01'", 
            required = false
        ) 
        String filter,
        
        @QueryParam("page") 
        @DefaultValue("1")
        @Parameter(description = "Page number", example = "1") 
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
        @Parameter(description = "Sort order", example = "desc") 
        String sortOrder
    ) {}
}

