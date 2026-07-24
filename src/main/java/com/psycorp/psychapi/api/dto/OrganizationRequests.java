package com.psycorp.psychapi.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

public final class OrganizationRequests {

    private OrganizationRequests() {}

    public record CreateOrganizationRequest(
        @Schema(
            description = "Nama organization",
            examples = "PT Company Name",
            required = true,
            maxLength = 100
        )
        @NotBlank(message = "Organization name is required")
        @Size(max = 100, message = "Organization name must not exceed 100 characters")
        String name,

        @Schema(
            description = "Deskripsi organization",
            examples = "Leading provider of solutions",
            required = false,
            maxLength = 500
        )
        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @Schema(
            description = "Website URL organization",
            examples = "https://company.com",
            required = false
        )
        String website,

        @Schema(
            description = "Nomor telepon organization",
            examples = "+622112345678",
            required = false
        )
        String phone,

        @Schema(
            description = "Email kontak organization",
            examples = "contact@company.com",
            required = false,
            format = "email"
        )
        @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Invalid email format")
        String email,

        @Schema(
            description = "Alamat organization",
            examples = "Jl. Sudirman No. 1, Jakarta",
            required = false,
            maxLength = 300
        )
        @Size(max = 300, message = "Address must not exceed 300 characters")
        String address
    ) {}

    public record UpdateOrganizationRequest(
        @Schema(
            description = "Nama organization",
            examples = "PT Updated Name",
            required = false,
            maxLength = 100
        )
        @Size(max = 100, message = "Organization name must not exceed 100 characters")
        String name,

        @Schema(
            description = "Deskripsi organization",
            examples = "Updated description",
            required = false,
            maxLength = 500
        )
        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @Schema(
            description = "Website URL organization",
            examples = "https://newwebsite.com",
            required = false
        )
        String website,

        @Schema(
            description = "Nomor telepon organization",
            examples = "+622198765432",
            required = false
        )
        String phone,

        @Schema(
            description = "Email kontak organization",
            examples = "newcontact@company.com",
            required = false,
            format = "email"
        )
        @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Invalid email format")
        String email,

        @Schema(
            description = "Alamat organization",
            examples = "New address",
            required = false,
            maxLength = 300
        )
        @Size(max = 300, message = "Address must not exceed 300 characters")
        String address
    ) {}

    public record DeleteOrganizationRequest(
        @Schema(
            description = "Confirmation text untuk delete organization. Harus 'DELETE_MY_ORGANIZATION'",
            examples = "DELETE_MY_ORGANIZATION",
            required = true
        )
        @NotBlank(message = "Confirmation is required")
        String confirmation
    ) {}

    public record OrganizationListRequest(
        @QueryParam("page")
        @DefaultValue("1")
        @Parameter(description = "Page number", example = "1")
        int page,

        @QueryParam("limit")
        @DefaultValue("10")
        @Parameter(description = "Items per page", example = "10") 
        int limit,

        @QueryParam("sortBy")
        @DefaultValue("createdAt")
        @Parameter(description = "Sort field", example = "createdAt")
        String sortBy,

        @QueryParam("sortOrder")
        @DefaultValue("desc")
        @Parameter(description = "Sort order", example = "desc")
        String sortOrder
    ) {}
}
