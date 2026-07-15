package com.psycorp.psychapi.common.util;
import org.bson.types.ObjectId;

import com.psycorp.psychapi.infrastructure.exception.ValidationException;

public final class ObjectIdValidator {

    private ObjectIdValidator() {} 
    
    public static ObjectId validate(String id) {
        try {
            return new ObjectId(id);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("INVALID_ID", "Invalid id format: " + id);
        }
    }
}
