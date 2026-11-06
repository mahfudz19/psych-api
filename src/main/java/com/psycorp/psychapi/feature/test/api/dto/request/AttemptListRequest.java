package com.psycorp.psychapi.feature.test.api.dto.request;

import java.util.List;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import com.psycorp.psychapi.shared.request.PageableRequest;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

public record AttemptListRequest(
        @QueryParam("search") String search,
        @QueryParam("filter") List<String> filter,
        @QueryParam("page") @DefaultValue("1") int page,
        @QueryParam("limit") @DefaultValue("20") int limit,
        @QueryParam("sortBy") @DefaultValue("startedAt") String sortBy,
        @QueryParam("sortOrder") @DefaultValue("desc") String sortOrder,

        @QueryParam("testId") @Parameter(description = "Filter by test ID") ObjectId testId,
        @QueryParam("status") @Parameter(description = "Filter by status: IN_PROGRESS, COMPLETED, ABANDONED, EXPIRED") String status)
        implements PageableRequest {
}