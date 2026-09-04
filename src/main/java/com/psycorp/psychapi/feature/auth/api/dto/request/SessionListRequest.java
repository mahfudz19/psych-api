package com.psycorp.psychapi.feature.auth.api.dto.request;

import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import com.psycorp.psychapi.shared.request.PageableRequest;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

public record SessionListRequest(
    
    @QueryParam("page")
    @DefaultValue("1")
    @Parameter(description = "Page number (1-based)", example = "1")
    int page,
    
    @QueryParam("limit")
    @DefaultValue("10")
    @Parameter(description = "Items per page", example = "10")
    int limit,
    
    @QueryParam("sortBy")
    @DefaultValue("lastActive")
    @Parameter(description = "Sort field", example = "lastActive")
    String sortBy,
    
    @QueryParam("sortOrder")
    @DefaultValue("desc")
    @Parameter(description = "Sort order: asc or desc", example = "desc")
    String sortOrder,
    
    @QueryParam("status")
    @Parameter(description = "Filter by status: active, revoked, expired, rotated", example = "active")
    String status
) implements PageableRequest {}
