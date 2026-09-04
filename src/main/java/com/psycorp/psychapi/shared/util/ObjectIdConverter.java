package com.psycorp.psychapi.shared.util;

import org.bson.types.ObjectId;

import com.psycorp.psychapi.infrastructure.exception.ValidationException;

import jakarta.ws.rs.ext.ParamConverter;

public class ObjectIdConverter implements ParamConverter<ObjectId> {
    @Override
    public ObjectId fromString(String value) {
        try { return new ObjectId(value); }
        catch (IllegalArgumentException e) {
            throw new ValidationException("INVALID_ID", "Invalid id format: " + value);
        }
    }
    
    @Override
    public String toString(ObjectId value) { return value.toHexString(); }
}