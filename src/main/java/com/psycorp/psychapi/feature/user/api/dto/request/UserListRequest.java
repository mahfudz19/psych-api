package com.psycorp.psychapi.feature.user.api.dto.request;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import com.psycorp.psychapi.shared.request.PageableRequest;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

public record UserListRequest(
    @QueryParam("search")
    @Parameter(description = "Search keyword in email, fullName, phone, bio", required = false)
    String search,

    @QueryParam("filter")
    @Parameter(description = "Filter: 'field:operator:value'. Example: 'status:in:active,suspended'", required = false)
    List<String> filter,
    
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
) implements PageableRequest {}
