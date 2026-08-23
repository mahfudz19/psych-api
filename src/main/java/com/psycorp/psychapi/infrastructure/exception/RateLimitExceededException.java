package com.psycorp.psychapi.infrastructure.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class RateLimitExceededException extends WebApplicationException {
    
    private final String code;
    private final String message;
    
    public RateLimitExceededException(String code, String message) {
        super(Response.Status.TOO_MANY_REQUESTS);
        this.code = code;
        this.message = message;
    }
    
    public RateLimitExceededException(String message) {
        this("RATE_LIMIT_EXCEEDED", message);
    }
    
    public String getCode() {
        return code;
    }
    
    @Override
    public String getMessage() {
        return message;
    }
}
