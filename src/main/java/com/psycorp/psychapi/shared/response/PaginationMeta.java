package com.psycorp.psychapi.shared.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public record PaginationMeta(
    int page,
    int limit,
    long total,
    int totalPages
) {
    public PaginationMeta(int page, int limit, long total, int totalPages) {
        this.page = page;
        this.limit = limit;
        this.total = total;
        this.totalPages = totalPages;
    }
    
    public static PaginationMeta of(int page, int limit, long total) {
        int totalPages = (int) Math.ceil((double) total / limit);
        return new PaginationMeta(page, limit, total, totalPages);
    }
}
