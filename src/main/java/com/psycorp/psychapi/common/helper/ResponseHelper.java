package com.psycorp.psychapi.common.helper;

import java.util.List;
import java.util.Map;

import com.psycorp.psychapi.common.response.ApiResponse;
import com.psycorp.psychapi.common.response.ErrorCode;
import com.psycorp.psychapi.common.response.FieldError;
import com.psycorp.psychapi.common.response.PaginationMeta;

import jakarta.ws.rs.core.Response;

public final class ResponseHelper {

    private ResponseHelper() {}

    // === SUCCESS ===

    public static <T> Response ok(T data) {
        return Response.ok(ApiResponse.success(data)).build();
    }

    public static <T> Response ok(T data, String message) {
        return Response.ok(ApiResponse.success(data, message)).build();
    }

    public static <T> Response ok(T data, String message, PaginationMeta meta) {
        return Response.ok(ApiResponse.success(data, message, meta)).build();
    }

    public static <T> Response created(T data) {
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(data, "Created"))
                .build();
    }

    public static <T> Response created(T data, String message) {
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(data, message))
                .build();
    }

    public static Response authenticationSuccess(Object userData, String token, long expiresIn, String message) {
        Map<String, Object> responseData = Map.of(
            "user", userData,
            "token", token,
            "expiresIn", expiresIn
        );
        
        // Set cookie dengan token
        String cookieValue = String.format(
            "auth_token=%s; Path=/; Max-Age=%d; HttpOnly; Secure; SameSite=Strict",
            token, expiresIn
        );
        
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(responseData, message))
                .header("Set-Cookie", cookieValue)
                .build();
    }

    public static Response authenticationOk(Object userData, String token, long expiresIn, String message) {
        Map<String, Object> responseData = Map.of(
            "user", userData,
            "token", token,
            "expiresIn", expiresIn
        );
        
        // Set cookie dengan token
        String cookieValue = String.format(
            "auth_token=%s; Path=/; Max-Age=%d; HttpOnly; Secure; SameSite=Strict",
            token, expiresIn
        );
        
        return Response.ok(ApiResponse.success(responseData, message))
                .header("Set-Cookie", cookieValue)
                .build();
    }

    public static Response noContent() {
        return Response.noContent().build();
    }

    // === ERROR ===

    public static Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponse.error(ErrorCode.BAD_REQUEST, message))
                .build();
    }

    public static Response badRequest(String message, List<FieldError> errors) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponse.error(ErrorCode.VALIDATION_ERROR, message, errors))
                .build();
    }

    public static Response notFound(String code, String message) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(ApiResponse.error(code, message))
                .build();
    }

    public static Response unauthorized(String message) {
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(ApiResponse.error(ErrorCode.UNAUTHORIZED, message))
                .build();
    }

    public static Response forbidden(String message) {
        return Response.status(Response.Status.FORBIDDEN)
                .entity(ApiResponse.error(ErrorCode.FORBIDDEN, message))
                .build();
    }

    public static Response internalServerError(String message) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR, message))
                .build();
    }
}
