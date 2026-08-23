package com.psycorp.psychapi.shared.response;

import java.time.Instant;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Standard API response wrapper untuk response yang sukses.
 */
@Schema(description = "Standard API response wrapper untuk response yang sukses")
@JsonInclude(Include.NON_NULL)
public record ApiResponse<T>(
    @Schema(description = "Status success/failure", examples = "true")
    boolean success,
    
    @Schema(description = "Response data payload")
    T data,
    
    @Schema(description = "Response message", examples = "Operation successful")
    String message,
    
    @Schema(description = "Pagination metadata (jika ada)")
    PaginationMeta meta,
    
    @Schema(description = "Response code", examples = "SUCCESS")
    String code,
    
    @Schema(description = "Response timestamp dalam ISO-8601 format", examples = "2024-01-15T10:30:00Z")
    Instant timestamp
) {
    // Static factory methods untuk berbagai skenario
    
    /**
     * Factory method untuk response OK (200) tanpa pagination.
     * @param data Data payload
     * @param message Response message
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message, null, null, Instant.now());
    }
    
    /**
     * Factory method untuk response OK (200) dengan pagination.
     * @param data Data payload
     * @param message Response message
     * @param meta Pagination metadata
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> ok(T data, String message, PaginationMeta meta) {
        return new ApiResponse<>(true, data, message, meta, null, Instant.now());
    }
    
    /**
     * Factory method untuk response Created (201).
     * @param data Data payload
     * @param message Response message
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> created(T data, String message) {
        return new ApiResponse<>(true, data, message, null, null, Instant.now());
    }
    
    /**
     * Factory method untuk response success tanpa data.
     * @param message Response message
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, null, message, null, null, Instant.now());
    }
}
