package com.psycorp.psychapi.feature.user.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

public record UpdateUserRequest(
    @Email(message = "Invalid email format")
    String email,
    
    String fullName,
    String phone,
    String bio,
    
    @Pattern(
        regexp = "^(active|inactive|suspended|deleted)$",
        message = "Status must be one of: active, inactive, suspended, deleted"
    )
    String status
) {}
