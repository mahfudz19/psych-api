package com.psycorp.psychapi.infrastructure.exception;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.psycorp.psychapi.common.helper.ResponseHelper;
import com.psycorp.psychapi.common.response.FieldError;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Exception mapper untuk menangani InvalidFormatException dari Jackson.
 * 
 * Mapper ini menangani error ketika nilai yang diberikan tidak valid untuk tipe target,
 * khususnya untuk enum values (misal: invalid accountType).
 */
@Provider
public class InvalidFormatExceptionMapper implements ExceptionMapper<InvalidFormatException> {

    @Override
    public Response toResponse(InvalidFormatException e) {
        String message = extractErrorMessage(e);
        String field = extractFieldName(e);
        
        // Create FieldError and pass as List (consistent with other mappers)
        List<FieldError> errors = new java.util.ArrayList<>();
        errors.add(new FieldError(field, message));
        return ResponseHelper.badRequest(message, errors);
    }
    
    /**
     * Extract error message yang user-friendly dari exception.
     */
    private String extractErrorMessage(InvalidFormatException e) {
        Object value = e.getValue();
        Class<?> targetType = e.getTargetType();
        
        if (targetType.isEnum()) {
            String[] enumValues = getEnumValues(targetType);
            return String.format(
                "Invalid value '%s' for enum %s. Valid values are: %s",
                value,
                targetType.getSimpleName(),
                String.join(", ", enumValues)
            );
        }
        
        return String.format(
            "Invalid value '%s' for field of type %s",
            value,
            targetType.getSimpleName()
        );
    }
    
    /**
     * Extract field name yang menyebabkan error.
     */
    private String extractFieldName(InvalidFormatException e) {
        StringBuilder path = new StringBuilder();
        
        // Build field path dari reference chain
        for (InvalidFormatException.Reference ref : e.getPath()) {
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
     * Get semua enum values sebagai array string.
     */
    private String[] getEnumValues(Class<?> targetType) {
        if (!targetType.isEnum()) {
            return new String[0];
        }
        
        Object[] constants = targetType.getEnumConstants();
        if (constants == null) {
            return new String[0];
        }
        
        return Arrays.stream(constants)
                .filter(e -> e != null)
                .map(e -> ((Enum<?>) e).name())
                .toArray(String[]::new);
    }
}
