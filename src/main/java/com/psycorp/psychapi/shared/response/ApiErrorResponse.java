package com.psycorp.psychapi.shared.response;

import java.time.Instant;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Standard API response wrapper untuk response yang error.
 */
@Schema(description = "Standard API response wrapper untuk response yang error")
@JsonInclude(Include.NON_NULL)
public record ApiErrorResponse(
    @Schema(description = "Status success/failure", examples = "false")
    boolean success,
    
    @Schema(description = "Error type", examples = "Bad Request")
    String error,
    
    @Schema(description = "Error message", examples = "Invalid input provided")
    String message,
    
    @Schema(description = "Error code", examples = "VALIDATION_ERROR")
    String code,
    
    @Schema(description = "List of field errors (untuk validation error)")
    List<FieldError> errors,
    
    @Schema(description = "Error timestamp dalam ISO-8601 format", examples = "2024-01-15T10:30:00Z")
    Instant timestamp
) {
    /**
     * Field error record untuk validation errors.
     */
    @Schema(description = "Field error detail")
    public record FieldError(
        @Schema(description = "Nama field yang error", examples = "email")
        String field,
        
        @Schema(description = "Pesan error untuk field ini", examples = "Email is required")
        String message
    ) {}
    
    /**
     * Factory method untuk single error.
     * @param error Error type
     * @param message Error message
     * @param path Request path
     * @return ApiErrorResponse
     */
    public static ApiErrorResponse of(String error, String message) {
        return new ApiErrorResponse(false, error, message, null, null, Instant.now());
    }

    /**
     * Factory method untuk error dengan code, error type, message, dan path.
     * @param code Error code (misal: "NOT_FOUND", "VALIDATION_ERROR")
     * @param error Error type (misal: "Not Found", "Bad Request")
     * @param message Pesan error detail
     * @param path Request path
     * @return ApiErrorResponse
     */
    public static ApiErrorResponse of(String code, String error, String message) {
        return new ApiErrorResponse(false, error, message, code, null, Instant.now());
    }
    
    /**
     * Factory method untuk validation errors (multiple fields).
     * @param errors List of field errors
     * @param path Request path
     * @return ApiErrorResponse
     */
    public static ApiErrorResponse validation(List<FieldError> errors) {
        return new ApiErrorResponse(false, "Validation Error", "Request validation failed", "VALIDATION_ERROR", errors, Instant.now());
    }
}
