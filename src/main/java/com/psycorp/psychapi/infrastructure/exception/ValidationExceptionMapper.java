package com.psycorp.psychapi.infrastructure.exception;

import java.util.List;

import com.psycorp.psychapi.common.helper.ResponseHelper;
import com.psycorp.psychapi.common.response.FieldError;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    @SuppressWarnings("null")
    public Response toResponse(ConstraintViolationException e) {
        List<FieldError> errors = e.getConstraintViolations().stream()
                .map(v -> new FieldError(
                        v.getPropertyPath().toString(),
                        v.getMessage()
                ))
                .toList();

        return ResponseHelper.badRequest("Validation failed", errors);
    }
}
