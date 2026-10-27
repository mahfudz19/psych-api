package com.psycorp.psychapi.feature.test.api.dto.request;

import java.util.List;

import org.bson.types.ObjectId;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import com.psycorp.psychapi.shared.request.PageableRequest;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

public record TestItemListRequest(
    @QueryParam("search") @Parameter(description = "Search keyword in "
        + SEARCH_FIELDS_LIST, required = false) String search,

    @QueryParam("filter") @Parameter(description = "Filter: 'field:operator:value'. Example: 'status:in:ACTIVE,RETIRED'", required = false) List<String> filter,

    @QueryParam("page") @DefaultValue("1") @Parameter(description = "Page number (1-based)", example = "1") int page,

    @QueryParam("limit") @DefaultValue("50") @Parameter(description = "Items per page (max 200)", example = "50") int limit,

    @QueryParam("sortBy") @DefaultValue("number") @Parameter(description = "Sort field", example = "number") String sortBy,

    @QueryParam("sortOrder") @DefaultValue("asc") @Parameter(description = "Sort order: asc or desc", example = "asc") String sortOrder,

    @QueryParam("sectionId") @Parameter(description = "Filter by section ID", required = false) ObjectId sectionId,

    @QueryParam("scaleId") @Parameter(description = "Filter by scale ID", required = false) ObjectId scaleId,

    @QueryParam("type") @Parameter(description = "Filter by item type", required = false) String type)
    implements PageableRequest {
  public static final String SEARCH_FIELDS_LIST = "content.text, tags";
  public static final String[] SEARCH_FIELDS = SEARCH_FIELDS_LIST.split(", ");
  public static final String DESCRIPTION = "Mengambil daftar soal pada alat tes dengan pagination. "
      + "Mendukung filter berdasarkan sectionId, scaleId, dan type.";
}
