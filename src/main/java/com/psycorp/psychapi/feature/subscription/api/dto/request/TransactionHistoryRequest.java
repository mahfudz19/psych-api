package com.psycorp.psychapi.feature.subscription.api.dto.request;

import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import com.psycorp.psychapi.shared.request.PageableRequest;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

public class TransactionHistoryRequest implements PageableRequest {
    
    public static final String[] SEARCH_FIELDS = {"referenceId", "paymentMethod"};

    @QueryParam("page")
    @DefaultValue("1")
    @Parameter(description = "Nomor halaman (dimulai dari 1)")
    private int page;

    @QueryParam("limit")
    @DefaultValue("10")
    @Parameter(description = "Jumlah data per halaman")
    private int limit;

    @QueryParam("search")
    @Parameter(description = "Pencarian berdasarkan referenceId atau paymentMethod")
    private String search;

    @QueryParam("sortBy")
    @DefaultValue("createdAt")
    @Parameter(description = "Field untuk pengurutan")
    private String sortBy;

    @QueryParam("sortOrder")
    @DefaultValue("desc")
    @Parameter(description = "Arah pengurutan (asc/desc)")
    private String sortOrder;

    @Override public int page() { return page; }
    @Override public int limit() { return limit; }
    @Override public String search() { return search; }
    @Override public String sortBy() { return sortBy; }
    @Override public String sortOrder() { return sortOrder; }
}