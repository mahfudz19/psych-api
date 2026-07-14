package com.psycorp.psychapi.common.response;

public record PaginationMeta(
    int page,
    int limit,
    long total,
    int totalPages
) {}

