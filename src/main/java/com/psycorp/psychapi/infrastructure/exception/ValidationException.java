package com.psycorp.psychapi.infrastructure.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class ValidationException extends WebApplicationException {
    
    public ValidationException(String code, String message) {
        super(Response.status(Response.Status.BAD_REQUEST)
            .entity(new ErrorResponse(code, message))
            .build());
    }
    
    private record ErrorResponse(String code, String message) {}
}
