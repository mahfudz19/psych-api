package com.example.psychapi.infrastructure.exception;

import com.example.psychapi.common.helper.ResponseHelper;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

    @Override
    public Response toResponse(NotFoundException e) {
        return ResponseHelper.notFound(e.getCode(), e.getMessage());
    }
}
