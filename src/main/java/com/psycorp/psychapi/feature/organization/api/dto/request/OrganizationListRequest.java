package com.psycorp.psychapi.feature.organization.api.dto.request;

import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

public record OrganizationListRequest(
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
