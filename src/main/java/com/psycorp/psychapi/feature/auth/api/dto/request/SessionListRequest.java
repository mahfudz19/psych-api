package com.psycorp.psychapi.feature.auth.api.dto.request;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import com.psycorp.psychapi.shared.request.PageableRequest;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

public record SessionListRequest(
    @QueryParam("search")
    @Parameter(description = "Search keyword in deviceName, browser, os, ipAddress", required = false)
    String search,

    @QueryParam("filter")
    @Parameter(description = "Filter: 'field:operator:value'. Example: 'status:in:active,revoked'", required = false)
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
    @DefaultValue("lastActive")
    @Parameter(description = "Sort field", example = "lastActive")
    String sortBy,

    @QueryParam("sortOrder")
    @DefaultValue("desc")
    @Parameter(description = "Sort order: asc or desc", example = "desc")
    String sortOrder
) implements PageableRequest {}