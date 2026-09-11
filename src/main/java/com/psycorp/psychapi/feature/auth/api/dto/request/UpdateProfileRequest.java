package com.psycorp.psychapi.feature.auth.api.dto.request;

import com.psycorp.psychapi.feature.user.model.User;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    String fullName,

    String phone,

    @Size(max = 500, message = "Bio must not exceed 500 characters")
    String bio,

    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date of birth must be in yyyy-MM-dd format")
    String dateOfBirth,

    @Pattern(regexp = "^(male|female)$", message = "Gender must be one of: male, female, other")
    User.Gender gender,

    String profilePicture
) {}