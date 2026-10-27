# Standar Implementasi Resource (API Endpoint)

Gunakan standar ini untuk membuat Controller/Resource JAX-RS di Quarkus.

## 1. Aturan Wajib (Hard Rules)

1. **Anotasi Class**: Wajib menggunakan `@Path`, `@Produces(MediaType.APPLICATION_JSON)`, `@Consumes(MediaType.APPLICATION_JSON)`, dan `@Tag`.
2. **Anotasi Method**: Wajib menggunakan `@GET`, `@POST`, `@PUT`, `@RolesAllowed` (jika butuh auth), dan `@Operation`.
3. **OpenAPI Responses**: Wajib mendefinisikan `@APIResponse` untuk HTTP 200/201, 400, 401, 403, 404, dan 500 menggunakan `ApiResponse.class` dan `ApiErrorResponse.class`.
4. **Return Type**: Selalu kembalikan `jakarta.ws.rs.core.Response` menggunakan `ResponseHelper.ok()`.
5. **Validasi Body**: Wajib gunakan `@Valid` pada parameter DTO untuk POST dan PUT.
6. **Validasi ID**: Untuk endpoint detail/update/delete, langsung gunakan tipe `org.bson.types.ObjectId` pada `@PathParam("id")`. Quarkus akan otomatis memvalidasi format Hex String-nya (melempar 404/400 jika format salah).

## 2. Template Resource (CRUD Lengkap)

```java
package com.psycorp.psychapi.feature.[domain].api;

import java.util.List;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.mongodb.client.model.Filters;
import com.psycorp.psychapi.shared.response.ApiErrorResponse;
import com.psycorp.psychapi.shared.response.ApiResponse;
import com.psycorp.psychapi.shared.response.PaginationMeta;
import com.psycorp.psychapi.shared.response.ResponseHelper;
import com.psycorp.psychapi.shared.util.MongoFilter;

import io.quarkus.mongodb.panache.PanacheQuery;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/[domain]")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "[Domain Name]", description = "[Domain Name] Management API")
public class EntityResource {

    @Inject
    EntityService entityService;

    @GET
    @RolesAllowed({"SUPERADMIN", "ADMIN"})
    @Operation(summary = "Get all data with pagination, search, and filter", description = EntityListRequest.DESCRIPTION)
    @APIResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @APIResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response getAll(@BeanParam EntityListRequest request) {
        Bson baseFilter = Filters.eq("deletedAt", null);
        Bson searchFilter = MongoFilter.fromRequest(request, EntityListRequest.SEARCH_FIELDS);
        Bson finalFilter = MongoFilter.and(baseFilter, searchFilter);
        Bson sort = MongoFilter.sort(request);

        PanacheQuery<Entity> query = entityService.find(finalFilter, sort).page(request.page() - 1, request.limit());
        long total = entityService.count(finalFilter);

        List<EntityResponse> data = query.stream().map(EntityResponse::fromEntity).toList();
        PaginationMeta meta = PaginationMeta.of(request, total);

        return ResponseHelper.ok(data, "Data retrieved successfully", meta);
    }

    @POST
    @RolesAllowed({"SUPERADMIN", "ADMIN"})
    @Operation(summary = "Create new data")
    @APIResponse(responseCode = "201", description = "Created", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @APIResponse(responseCode = "400", description = "Bad Request (Validation/Duplicate)", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response create(@Valid EntityCreateRequest request) {
        Entity entity = entityService.create(request);
        return ResponseHelper.ok(EntityResponse.fromEntity(entity), "Data berhasil dibuat");
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"SUPERADMIN", "ADMIN"})
    @Operation(summary = "Update existing data")
    @APIResponse(responseCode = "200", description = "Updated", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @APIResponse(responseCode = "400", description = "Bad Request (Validation)", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "404", description = "Not Found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response update(@PathParam("id") ObjectId id, @Valid EntityUpdateRequest request) {
        Entity entity = entityService.update(id, request);
        return ResponseHelper.ok(EntityResponse.fromEntity(entity), "Data berhasil diperbarui");
    }
}
```
