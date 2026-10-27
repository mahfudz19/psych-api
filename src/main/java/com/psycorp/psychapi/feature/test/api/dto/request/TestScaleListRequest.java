package com.psycorp.psychapi.feature.test.api.dto.request;

import java.util.List;

import org.bson.types.ObjectId;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import com.psycorp.psychapi.shared.request.PageableRequest;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

public record TestScaleListRequest(
    @QueryParam("search") @Parameter(description = "Search keyword in "
        + SEARCH_FIELDS_LIST, required = false) String search,

    @QueryParam("filter") @Parameter(description = "Filter: 'field:operator:value'. Example: 'type:eq:DIMENSION'", required = false) List<String> filter,

    @QueryParam("sectionId") @Parameter(description = "Filter by Section ID", required = false) ObjectId sectionId,

    @QueryParam("page") @DefaultValue("1") @Parameter(description = "Page number (1-based)", example = "1") int page,

    @QueryParam("limit") @DefaultValue("10") @Parameter(description = "Items per page", example = "10") int limit,

    @QueryParam("sortBy") @DefaultValue("order") @Parameter(description = "Sort field", example = "order") String sortBy,

    @QueryParam("sortOrder") @DefaultValue("asc") @Parameter(description = "Sort order: asc or desc", example = "asc") String sortOrder)
    implements PageableRequest {
  public static final String SEARCH_FIELDS_LIST = "code, name, description";
  public static final String[] SEARCH_FIELDS = SEARCH_FIELDS_LIST.split(", ");
  public static final String DESCRIPTION = "Mengambil daftar skala pada alat tes dengan pagination. Mendukung pencarian keyword di field "
      + SEARCH_FIELDS_LIST + ". Mendukung filter dinamis dengan format 'field:operator:value'.";
}
