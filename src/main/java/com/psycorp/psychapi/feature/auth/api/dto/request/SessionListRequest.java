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
) implements PageableRequest {
    public static final String[] SESSION_SEARCH_FIELDS = { "deviceInfo.browser", "deviceInfo.os", "deviceInfo.ip", "deviceInfo.location" };
    private static final List<String> DEVICE_FIELDS = List.of("lastActive", "browser", "os", "ip", "location");

    @Override
    public String sortBy() {
        if (sortBy != null) {
            for (String field : DEVICE_FIELDS) {
                if (field.equalsIgnoreCase(sortBy)) {
                    return "deviceInfo." + field;
                }
            }
        }
        return sortBy;
    }

    @Override
    public List<String> filter() {
        if (filter == null) {
            return List.of();
        }
        return filter.stream()
            .map(f -> {
                if (f == null) return null;
                for (String field : DEVICE_FIELDS) {
                    if (f.startsWith(field + ":")) {
                        return "deviceInfo." + f;
                    }
                }
                return f;
            })
            .toList();
    }
}