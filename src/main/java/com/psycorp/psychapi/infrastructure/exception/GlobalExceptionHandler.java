package com.psycorp.psychapi.infrastructure.exception;

import java.util.List;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.psycorp.psychapi.shared.response.ApiErrorResponse;
import com.psycorp.psychapi.shared.response.ApiErrorResponse.FieldError;

import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Global exception handler menggunakan RESTEasy Reactive @ServerExceptionMapper.
 * Semua exception yang dilempar di aplikasi akan ditangkap dan dikonversi ke
 * format ApiErrorResponse yang konsisten.
 */
public class GlobalExceptionHandler {

    @Inject
    Logger log;

    /**
     * 1. Menangkap ConstraintViolationException dari validasi @Valid
     * Status: 400 Bad Request
     */
    @ServerExceptionMapper
    public Response mapConstraintViolation(ConstraintViolationException ex) {
        List<FieldError> errors = ex.getConstraintViolations().stream()
                .map(violation -> new FieldError(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ))
                .collect(Collectors.toList());

        ApiErrorResponse errorResponse = ApiErrorResponse.validation(errors);
        
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorResponse)
                .build();
    }

    /**
     * 2. Menangkap custom NotFoundException
     * Status: 404 Not Found
     */
    @ServerExceptionMapper
    public Response mapNotFoundException(NotFoundException ex) {
        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                "NOT_FOUND",
                "Not Found",
                ex.getMessage()
        );
        
        return Response.status(Response.Status.NOT_FOUND)
                .entity(errorResponse)
                .build();
    }

    /**
     * 3. Menangkap custom NotFoundException (dari package infrastructure)
     * Status: 404 Not Found
     */
    @ServerExceptionMapper
    public Response mapNotFoundException(com.psycorp.psychapi.infrastructure.exception.NotFoundException ex) {
        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                ex.getCode(),
                "Not Found",
                ex.getMessage()
        );
        
        return Response.status(Response.Status.NOT_FOUND)
                .entity(errorResponse)
                .build();
    }

    /**
     * 4. Menangkap custom ValidationException
     * Status: 400 Bad Request
     */
    @ServerExceptionMapper
    public Response mapValidationException(ValidationException ex) {
        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                ex.getCode(),
                "Bad Request",
                ex.getMessage()
        );
        
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorResponse)
                .build();
    }

    /**
     * 5. Menangkap custom RateLimitExceededException
     * Status: 429 Too Many Requests
     */
    @ServerExceptionMapper
    public Response mapRateLimitExceededException(RateLimitExceededException ex) {
        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                ex.getCode(),
                "Too Many Requests",
                ex.getMessage()
        );
        
        return Response.status(Response.Status.TOO_MANY_REQUESTS)
                .entity(errorResponse)
                .build();
    }

    /**
     * 6. Menangkap NotAuthorizedException (401)
     * Status: 401 Unauthorized
     */
    @ServerExceptionMapper
    public Response mapNotAuthorizedException(NotAuthorizedException ex) {
        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                "UNAUTHORIZED",
                "Unauthorized",
                "Authentication required"
        );
        
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(errorResponse)
                .build();
    }

    /**
     * 7. Menangkap ForbiddenException (403)
     * Status: 403 Forbidden
     */
    @ServerExceptionMapper
    public Response mapForbiddenException(ForbiddenException ex) {
        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                "FORBIDDEN",
                "Forbidden",
                "You don't have permission to access this resource"
        );
        
        return Response.status(Response.Status.FORBIDDEN)
                .entity(errorResponse)
                .build();
    }

    /**
     * 8. Menangkap BadRequestException bawaan JAX-RS (400)
     * Status: 400 Bad Request
     */
    @ServerExceptionMapper
    public Response mapBadRequestException(BadRequestException ex) {
        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                "BAD_REQUEST",
                "Bad Request",
                ex.getMessage() != null ? ex.getMessage() : "Invalid request"
        );
        
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorResponse)
                .build();
    }

    /**
     * 9. Menangkap WebApplicationException lainnya (4xx)
     * Status: Sesuai status code dari exception
     */
    @ServerExceptionMapper
    public Response mapWebApplicationException(WebApplicationException ex) {
        int status = ex.getResponse().getStatus();
        
        // Cek apakah cause-nya adalah JsonProcessingException
        Throwable cause = ex.getCause();
        String message;
        String code = "HTTP_ERROR_" + status;
        
        if (cause instanceof InvalidFormatException ife) {
            // Invalid enum value atau format error
            message = "Invalid value for field '%s': %s".formatted(
                ife.getPath().isEmpty() ? "unknown" : ife.getPath().get(0).getFieldName(),
                ife.getValue() != null ? ife.getValue().toString() : "null"
            );
            code = "INVALID_FORMAT";
        } else if (cause instanceof JsonProcessingException jpe) {
            // General JSON processing error
            message = "Invalid JSON format: %s".formatted(jpe.getOriginalMessage());
            code = "INVALID_JSON";
        } else if (cause instanceof IllegalArgumentException iae) {
            // Invalid enum dari custom deserializer
            message = iae.getMessage() != null ? iae.getMessage() : "Invalid argument provided";
            code = "INVALID_ARGUMENT";
        } else {
            message = ex.getMessage() != null ? ex.getMessage() : "Request failed";
        }
        
        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                code,
                getErrorTypeForStatus(status),
                message
        );
        
        return Response.status(status)
                .entity(errorResponse)
                .build();
    }

    /**
     * 10. Catch-All: Menangkap semua exception yang tidak tertangkap
     * Status: 500 Internal Server Error
     * 
     * PENTING: Log stack trace untuk debugging, tapi jangan bocorkan ke client
     */
    @ServerExceptionMapper
    public Response mapGenericException(Exception ex) {
        // Log full stack trace untuk debugging
        log.error("Unhandled Exception caught:", ex);
        
        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                "INTERNAL_ERROR",
                "Internal Server Error",
                "Terjadi kesalahan internal pada server"
        );
        
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse)
                .build();
    }

    /**
     * 11. Menangkap error deserialization Jackson (misal: invalid enum value)
     * Status: 400 Bad Request
     */
    @ServerExceptionMapper
    public Response mapJsonProcessingException(JsonProcessingException ex) {
    String message = "Invalid JSON format";
    
    // Jika ini InvalidFormatException (enum invalid, dll)
    if (ex instanceof InvalidFormatException ife) {
            message = "Invalid value for field '%s': %s".formatted(
            ife.getPath().isEmpty() ? "unknown" : ife.getPath().get(0).getFieldName(),
            ife.getValue() != null ? ife.getValue().toString() : "null"
            );
    }
    
    ApiErrorResponse errorResponse = ApiErrorResponse.of(
            "INVALID_JSON",
            "Bad Request",
            message
    );
    
    return Response.status(Response.Status.BAD_REQUEST)
            .entity(errorResponse)
            .build();
    }


    /**
     * Helper method untuk mapping status code ke error type string.
     */
    private String getErrorTypeForStatus(int status) {
        return switch (status) {
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 409 -> "Conflict";
            case 422 -> "Unprocessable Entity";
            case 429 -> "Too Many Requests";
            case 500 -> "Internal Server Error";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            default -> "HTTP Error " + status;
        };
    }
}
