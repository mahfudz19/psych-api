package com.psycorp.psychapi.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.QueryParam;

public final class PostRequests {
    private PostRequests() {}

    public record CreatePostRequest(
        @NotBlank String title,
        String content,
        @Pattern(regexp = "^(draft|published)$") String status
    ) {}

    public record UpdatePostRequest(
        String title,
        String content,
        @Pattern(regexp = "^(draft|published)$") String status
    ) {}

    public record PostListRequest(
        @QueryParam("page") Integer page,
        @QueryParam("limit") Integer limit,
        @QueryParam("search") String search,
        @QueryParam("sortBy") String sortBy,
        @QueryParam("sortOrder") String sortOrder
    ) {}
}

