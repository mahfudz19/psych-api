package com.example.psychapi.common.response;

public record ApiResponse(
    boolean success,
    Object data,
    String message,
    PaginationMeta meta,
    String code,
    java.util.List<FieldError> errors
) {
    public static ApiResponse success(Object data) {
        return new ApiResponse(true, data, null, null, null, null);
    }

    public static ApiResponse success(Object data, String message) {
        return new ApiResponse(true, data, message, null, null, null);
    }

    public static ApiResponse success(Object data, PaginationMeta meta) {
        return new ApiResponse(true, data, null, meta, null, null);
    }

    public static ApiResponse success(Object data, String message, PaginationMeta meta) {
        return new ApiResponse(true, data, message, meta, null, null);
    }

    public static ApiResponse error(String code, String message) {
        return new ApiResponse(false, null, message, null, code, null);
    }

    public static ApiResponse error(String code, String message, java.util.List<FieldError> errors) {
        return new ApiResponse(false, null, message, null, code, errors);
    }
}
