package com.psycorp.psychapi.infrastructure.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Custom validation exception untuk error validasi business logic.
 * 
 * Exception ini akan dihandle oleh ValidationExceptionMapper untuk menghasilkan
 * response yang konsisten dengan format ApiResponse.
 */
public class ValidationException extends WebApplicationException {
    
    private final String code;
    private final String message;
    
    public ValidationException(String code, String message) {
        super(Response.Status.BAD_REQUEST);
        this.code = code;
        this.message = message;
    }
    
    public String getCode() {
        return code;
    }
    
    @Override
    public String getMessage() {
        return message;
    }
}
