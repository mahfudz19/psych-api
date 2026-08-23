package com.psycorp.psychapi.shared.util;

import org.bson.types.ObjectId;

import com.psycorp.psychapi.infrastructure.exception.ValidationException;

/**
 * Utility class untuk validasi umum.
 * 
 * <p>Usage Examples:</p>
 * <pre>
 * // Validate ObjectId format
 * ObjectId id = ValidationUtils.validateObjectId("507f1f77bcf86cd799439011");
 * 
 * // Validate string not blank
 * ValidationUtils.requireNotBlank(name, "Name is required");
 * </pre>
 */
public final class ValidationUtils {
    
    private ValidationUtils() {
        // Prevent instantiation
    }
    
    /**
     * Validate string ObjectId dan convert ke ObjectId object.
     * 
     * @param id String ObjectId yang akan divalidasi
     * @return ObjectId object
     * @throws ValidationException jika format id tidak valid
     */
    public static ObjectId validateObjectId(String id) {
        try {
            return new ObjectId(id);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("INVALID_ID", "Invalid id format: " + id);
        }
    }
    
    /**
     * Validate string tidak null dan tidak blank.
     * 
     * @param value String yang akan divalidasi
     * @param fieldName Nama field untuk pesan error
     * @throws ValidationException jika value null atau blank
     */
    public static void requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("VALIDATION_ERROR", fieldName + " tidak boleh kosong");
        }
    }
    
    /**
     * Validate object tidak null.
     * 
     * @param value Object yang akan divalidasi
     * @param fieldName Nama field untuk pesan error
     * @throws ValidationException jika value null
     */
    public static void requireNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new ValidationException("VALIDATION_ERROR", fieldName + " tidak boleh null");
        }
    }
    
    /**
     * Validate boolean condition.
     * 
     * @param condition Condition yang harus true
     * @param message Pesan error jika condition false
     * @throws ValidationException jika condition false
     */
    public static void requireTrue(boolean condition, String message) {
        if (!condition) {
            throw new ValidationException("VALIDATION_ERROR", message);
        }
    }
}
