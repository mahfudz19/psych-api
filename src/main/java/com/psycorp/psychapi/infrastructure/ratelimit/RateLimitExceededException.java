package com.psycorp.psychapi.infrastructure.ratelimit;

import com.psycorp.psychapi.infrastructure.exception.ValidationException;

/**
 * Exception yang dilempar ketika rate limit exceeded.
 * HTTP 429 (Too Many Requests) akan di-return ke client.
 * 
 * @author Architect
 */
public class RateLimitExceededException extends ValidationException {
    
    /**
     * Constructor untuk RateLimitExceededException.
     * 
     * @param errorCode Kode error untuk identifikasi
     * @param message Pesan error yang deskriptif
     */
    public RateLimitExceededException(String errorCode, String message) {
        super(errorCode, message);
    }
    
    /**
     * Constructor dengan default error code.
     * 
     * @param message Pesan error yang deskriptif
     */
    public RateLimitExceededException(String message) {
        super("RATE_LIMIT_EXCEEDED", message);
    }
}
