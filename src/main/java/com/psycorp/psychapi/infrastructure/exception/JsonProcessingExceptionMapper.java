package com.psycorp.psychapi.infrastructure.exception;

import java.util.List;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.psycorp.psychapi.common.helper.ResponseHelper;
import com.psycorp.psychapi.common.response.FieldError;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Exception mapper untuk menangani Jackson JSON processing exceptions.
 * 
 * Mapper ini menangani error yang terjadi saat deserialisasi JSON ke Java object,
 * seperti invalid enum values, type mismatches, dan malformed JSON.
 */
@Provider
public class JsonProcessingExceptionMapper implements ExceptionMapper<JsonMappingException> {

    @Override
    public Response toResponse(JsonMappingException e) {
        String message = extractErrorMessage(e);
        String field = extractFieldName(e);
        
        // Create FieldError and pass as List (consistent with ValidationExceptionMapper)
        List<FieldError> errors = List.of(new FieldError(field, message));
        return ResponseHelper.badRequest(message, errors);
    }
    
    /**
     * Extract error message yang user-friendly dari exception.
     */
    private String extractErrorMessage(JsonMappingException e) {
        // Handle invalid enum value
        if (e instanceof InvalidFormatException ex) {
            String value = ex.getValue().toString();
            Class<?> targetType = ex.getTargetType();
            
            if (targetType.isEnum()) {
                return String.format(
                    "Invalid value '%s' for enum %s. Valid values are: %s",
                    value,
                    targetType.getSimpleName(),
                    String.join(", ", getEnumValues(targetType))
                );
            }
            
            return String.format(
                "Invalid value '%s' for field of type %s",
                value,
                targetType.getSimpleName()
            );
        }
        
        // Handle type mismatch
        if (e instanceof MismatchedInputException) {
            return "Type mismatch: expected " + getExpectedType(e) + " but got different type";
        }
        
        // Default message
        String originalMsg = e.getOriginalMessage();
        if (originalMsg != null && !originalMsg.isEmpty()) {
            // Remove stack trace from message
            return originalMsg.split(" at ")[0];
        }
        
        return "Invalid JSON format";
    }
    
    /**
     * Extract field name yang menyebabkan error.
     */
    private String extractFieldName(JsonMappingException e) {
        StringBuilder path = new StringBuilder();
        
        // Build field path dari reference chain
        for (JsonMappingException.Reference ref : e.getPath()) {
            if (ref.getFieldName() != null) {
                if (path.length() > 0) {
                    path.append(".");
                }
                path.append(ref.getFieldName());
            } else if (ref.getIndex() >= 0) {
                path.append("[").append(ref.getIndex()).append("]");
            }
        }
        
        return path.length() > 0 ? path.toString() : null;
    }
    
    /**
     * Get expected type dari exception.
     */
    private String getExpectedType(JsonMappingException e) {
        if (e instanceof MismatchedInputException ex) {
            if (ex.getTargetType() != null) {
                return ex.getTargetType().getSimpleName();
            }
        }
        return "unknown";
    }
    
    /**
     * Get semua enum values sebagai array string.
     */
    @SuppressWarnings("unchecked")
    private String[] getEnumValues(Class<?> targetType) {
        if (targetType.isEnum()) {
            Enum<?>[] enums = (Enum<?>[]) targetType.getEnumConstants();
            String[] names = new String[enums.length];
            for (int i = 0; i < enums.length; i++) {
                names[i] = enums[i].name();
            }
            return names;
        }
        return new String[0];
    }
}
