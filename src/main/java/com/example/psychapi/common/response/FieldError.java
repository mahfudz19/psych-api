package com.example.psychapi.common.response;

public record FieldError(
    String field,
    String message
) {}