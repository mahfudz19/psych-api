package com.psycorp.psychapi.shared.response;

import java.util.List;

import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

public class ResponseHelper {

    // === SUCCESS RESPONSES ===
    
    public static <T> Response ok(T data, String message) {
        return Response.ok(ApiResponse.ok(data, message)).build();
    }
    
    public static <T> Response ok(T data, String message, PaginationMeta meta) {
        return Response.ok(ApiResponse.ok(data, message, meta)).build();
    }
    
    public static <T> Response created(T data, String message) {
        return Response.status(Response.Status.CREATED).entity(ApiResponse.created(data, message)).build();
    }
    
    public static Response success(String message) {
        return Response.ok(ApiResponse.success(message)).build();
    }
    
    // === SUCCESS WITH COOKIES ===
    
    public static <T> Response ok(T data, String message, List<NewCookie> cookies) {
        Response.ResponseBuilder builder = Response.ok(ApiResponse.ok(data, message));
        cookies.forEach(builder::cookie);
        return builder.build();
    }
    
    public static <T> Response created(T data, String message, List<NewCookie> cookies) {
        Response.ResponseBuilder builder = Response.status(Response.Status.CREATED).entity(ApiResponse.created(data, message));
        cookies.forEach(builder::cookie);
        return builder.build();
    }
    
    // === ERROR RESPONSES ===
    
    public static Response badRequest(String message, String path) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(ApiErrorResponse.of("Bad Request", message, path))
            .build();
    }
    
    public static Response unauthorized(String message, String path) {
        return Response.status(Response.Status.UNAUTHORIZED)
            .entity(ApiErrorResponse.of("Unauthorized", message, path))
            .build();
    }
    
    public static Response forbidden(String message, String path) {
        return Response.status(Response.Status.FORBIDDEN)
            .entity(ApiErrorResponse.of("Forbidden", message, path))
            .build();
    }
    
    public static Response notFound(String message, String path) {
        return Response.status(Response.Status.NOT_FOUND)
            .entity(ApiErrorResponse.of("Not Found", message, path))
            .build();
    }
    
    public static Response conflict(String message, String path) {
        return Response.status(Response.Status.CONFLICT)
            .entity(ApiErrorResponse.of("Conflict", message, path))
            .build();
    }
    
    public static Response validationError(List<ApiErrorResponse.FieldError> errors, String path) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(ApiErrorResponse.validation(errors))
            .build();
    }
    
    public static Response internalError(String message, String path) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(ApiErrorResponse.of("Internal Server Error", message, path))
            .build();
    }
}
