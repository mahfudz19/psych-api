package com.psycorp.psychapi.infrastructure.exception;

import com.psycorp.psychapi.common.helper.ResponseHelper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception e) {
        return ResponseHelper.internalServerError(e.getMessage());
    }
}
