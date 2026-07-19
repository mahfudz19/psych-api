package com.psycorp.psychapi.infrastructure.exception;

import java.util.List;

import com.psycorp.psychapi.common.helper.ResponseHelper;
import com.psycorp.psychapi.common.response.FieldError;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Exception mapper untuk menangani custom ValidationException.
 * 
 * Mapper ini mengubah exception validasi business logic menjadi response JSON
 * yang konsisten dengan format ApiResponse.
 */
@Provider
public class CustomValidationExceptionMapper implements ExceptionMapper<ValidationException> {

    @Override
    public Response toResponse(ValidationException e) {
        // Create FieldError dengan message dari exception
        List<FieldError> errors = List.of(new FieldError(null, e.getMessage()));
        return ResponseHelper.badRequest(e.getMessage(), errors);
    }
}
