package com.psycorp.psychapi.feature.test.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.mongodb.client.model.Filters;
import com.psycorp.psychapi.feature.test.api.dto.request.NormCreateRequest;
import com.psycorp.psychapi.feature.test.api.dto.request.NormUpdateRequest;
import com.psycorp.psychapi.feature.test.api.dto.request.TestDefinitionCreateRequest;
import com.psycorp.psychapi.feature.test.api.dto.request.TestDefinitionListRequest;
import com.psycorp.psychapi.feature.test.api.dto.request.TestDefinitionUpdateRequest;
import com.psycorp.psychapi.feature.test.api.dto.request.TestItemCreateRequest;
import com.psycorp.psychapi.feature.test.api.dto.request.TestItemListRequest;
import com.psycorp.psychapi.feature.test.api.dto.request.TestItemUpdateRequest;
import com.psycorp.psychapi.feature.test.api.dto.request.TestScaleCreateRequest;
import com.psycorp.psychapi.feature.test.api.dto.request.TestScaleListRequest;
import com.psycorp.psychapi.feature.test.api.dto.request.TestScaleUpdateRequest;
import com.psycorp.psychapi.feature.test.api.dto.request.TestSectionCreateRequest;
import com.psycorp.psychapi.feature.test.api.dto.request.TestSectionListRequest;
import com.psycorp.psychapi.feature.test.api.dto.request.TestSectionUpdateRequest;
import com.psycorp.psychapi.feature.test.api.dto.response.NormTableResponse;
import com.psycorp.psychapi.feature.test.api.dto.response.TestDefinitionResponse;
import com.psycorp.psychapi.feature.test.api.dto.response.TestDetailResponse;
import com.psycorp.psychapi.feature.test.api.dto.response.TestItemResponse;
import com.psycorp.psychapi.feature.test.api.dto.response.TestScaleResponse;
import com.psycorp.psychapi.feature.test.api.dto.response.TestSectionResponse;
import com.psycorp.psychapi.feature.test.models.NormTable;
import com.psycorp.psychapi.feature.test.models.TestDefinition;
import com.psycorp.psychapi.feature.test.models.TestItem;
import com.psycorp.psychapi.feature.test.models.TestScale;
import com.psycorp.psychapi.feature.test.models.TestSection;
import com.psycorp.psychapi.feature.test.service.NormTableService;
import com.psycorp.psychapi.feature.test.service.TestCatalogService;
import com.psycorp.psychapi.feature.test.service.TestDefinitionService;
import com.psycorp.psychapi.feature.test.service.TestItemService;
import com.psycorp.psychapi.feature.test.service.TestScaleService;
import com.psycorp.psychapi.feature.test.service.TestSectionService;
import com.psycorp.psychapi.shared.response.PaginationMeta;
import com.psycorp.psychapi.shared.response.ResponseHelper;
import com.psycorp.psychapi.shared.util.MongoFilter;

import io.quarkus.mongodb.panache.PanacheQuery;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/admin/tests")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Test Definition", description = "Test Definition Management API")
public class TestDefinitionResource {

    @Inject
    TestDefinitionService testDefinitionService;

    @Inject
    TestCatalogService testCatalogService;

    @Inject
    TestSectionService sectionService;

    @Inject
    TestScaleService scaleService;

    @Inject
    TestItemService itemService;

    @Inject
    NormTableService normService;

    @GET
    @RolesAllowed({ "SUPERADMIN" })
    @Operation(summary = "Get all test definitions")
    public Response getAll(@BeanParam TestDefinitionListRequest request) {
        Bson baseFilter = Filters.eq("deletedAt", null);
        Bson searchFilter = MongoFilter.fromRequest(request, TestDefinitionListRequest.SEARCH_FIELDS);
        Bson finalFilter = MongoFilter.and(baseFilter, searchFilter);
        Bson sort = MongoFilter.sort(request);

        PanacheQuery<TestDefinition> query = testDefinitionService.find(finalFilter, sort).page(request.page() - 1,
                request.limit());
        long total = testDefinitionService.count(finalFilter);

        List<TestDefinitionResponse> data = query.stream().map(TestDefinitionResponse::fromEntity).toList();
        return ResponseHelper.ok(data, "Test definitions retrieved successfully", PaginationMeta.of(request, total));
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({ "SUPERADMIN" })
    @Operation(summary = "Get test detail with stats and progress")
    public Response getById(@PathParam("id") ObjectId id) {
        Map<String, Object> detail = testCatalogService.getDetail(id);
        return ResponseHelper.ok(TestDetailResponse.fromMap(detail), "Test detail retrieved successfully");
    }

    @POST
    @RolesAllowed({ "SUPERADMIN" })
    @Operation(summary = "Create new test definition")
    public Response create(@Valid TestDefinitionCreateRequest request) {
        TestDefinition entity = testDefinitionService.create(request);
        return ResponseHelper.created(TestDefinitionResponse.fromEntity(entity), "Test definition berhasil dibuat");
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({ "SUPERADMIN" })
    @Operation(summary = "Update existing test definition")
    public Response update(@PathParam("id") ObjectId id, @Valid TestDefinitionUpdateRequest request) {
        TestDefinition entity = testDefinitionService.update(id, request);
        return ResponseHelper.ok(TestDefinitionResponse.fromEntity(entity), "Test definition berhasil diperbarui");
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({ "SUPERADMIN" })
    @Operation(summary = "Soft delete test and all its contents")
    public Response delete(@PathParam("id") ObjectId id) {
        testCatalogService.deleteTest(id);
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/publish")
    @RolesAllowed({ "SUPERADMIN" })
    @Operation(summary = "Publish test (DRAFT to ACTIVE)")
    public Response publish(@PathParam("id") ObjectId id) {
        TestDefinition entity = testDefinitionService.publish(id);
        return ResponseHelper.ok(TestDefinitionResponse.fromEntity(entity), "Test berhasil dipublish");
    }

    @POST
    @Path("/{id}/archive")
    @RolesAllowed({ "SUPERADMIN" })
    @Operation(summary = "Archive test (ACTIVE to ARCHIVED)")
    public Response archive(@PathParam("id") ObjectId id) {
        TestDefinition entity = testDefinitionService.archive(id);
        return ResponseHelper.ok(TestDefinitionResponse.fromEntity(entity), "Test berhasil diarsipkan");
    }

    @GET
    @Path("/{testId}/sections")
    @RolesAllowed({ "SUPERADMIN" })
    @Operation(summary = "Get all sections for a test")
    public Response getAll(@PathParam("testId") ObjectId testId, @BeanParam TestSectionListRequest request) {
        Bson baseFilter = Filters.and(Filters.eq("testId", testId), Filters.eq("deletedAt", null));
        Bson searchFilter = MongoFilter.fromRequest(request, TestSectionListRequest.SEARCH_FIELDS);
        Bson finalFilter = MongoFilter.and(baseFilter, searchFilter);
        Bson sort = MongoFilter.sort(request);

        PanacheQuery<TestSection> query = sectionService.find(finalFilter, sort).page(request.page() - 1,
                request.limit());
        long total = sectionService.count(finalFilter);

        List<TestSectionResponse> data = query.stream().map(TestSectionResponse::fromEntity).toList();
        return ResponseHelper.ok(data, "Test sections retrieved successfully", PaginationMeta.of(request, total));
    }

    @GET
    @Path("/{testId}/sections/{sectionId}")
    @RolesAllowed({ "SUPERADMIN" })
    @Operation(summary = "Get section detail")
    public Response getSectionsById(@PathParam("testId") ObjectId testId, @PathParam("sectionId") ObjectId sectionId) {
        TestSection section = sectionService.getInTest(testId, sectionId);
        return ResponseHelper.ok(TestSectionResponse.fromEntity(section), "Section retrieved successfully");
    }

    @POST
    @Path("/{testId}/sections")
    @RolesAllowed({ "SUPERADMIN" })
    @Operation(summary = "Create new section")
    public Response create(@PathParam("testId") ObjectId testId, @Valid TestSectionCreateRequest req) {
        TestSection section = sectionService.create(
                testId, req.code(), req.order(), req.name(), req.instructions(),
                req.examples(), req.showExamplesFirst(), req.timeLimitSeconds(),
                req.itemCount(), req.navigationPolicy(), req.defaultItemType(),
                req.stimulusPhase(), req.config(), req.status());
        return ResponseHelper.created(TestSectionResponse.fromEntity(section), "Section berhasil dibuat");
    }

    @PUT
    @Path("/{testId}/sections/{sectionId}")
    @RolesAllowed({ "SUPERADMIN" })
    @Operation(summary = "Update existing section")
    public Response update(@PathParam("testId") ObjectId testId, @PathParam("sectionId") ObjectId sectionId,
            @Valid TestSectionUpdateRequest req) {
        TestSection section = sectionService.update(
                testId, sectionId, req.order(), req.name(), req.instructions(),
                req.examples(), req.showExamplesFirst(), req.timeLimitSeconds(),
                req.itemCount(), req.navigationPolicy(), req.defaultItemType(),
                req.stimulusPhase(), req.config(), req.status());
        return ResponseHelper.ok(TestSectionResponse.fromEntity(section), "Section berhasil diperbarui");
    }

    @DELETE
    @Path("/{testId}/sections/{sectionId}")
    @RolesAllowed({ "SUPERADMIN" })
    @Operation(summary = "Delete section")
    public Response deleteSection(@PathParam("testId") ObjectId testId, @PathParam("sectionId") ObjectId sectionId) {
        testCatalogService.deleteSection(testId, sectionId);
        return Response.noContent().build();
    }

    @GET
    @Path("/{testId}/scales")
    @RolesAllowed({ "SUPERADMIN" })
    @Operation(summary = "Get all scales for a test")
    public Response getAll(@PathParam("testId") ObjectId testId, @BeanParam TestScaleListRequest request) {
        List<Bson> baseFilters = new ArrayList<>();
        baseFilters.add(Filters.eq("testId", testId));
        baseFilters.add(Filters.eq("deletedAt", null));
        if (request.sectionId() != null) {
            baseFilters.add(Filters.eq("sectionId", request.sectionId()));
        }

        Bson baseFilter = Filters.and(baseFilters);
        Bson searchFilter = MongoFilter.fromRequest(request, TestScaleListRequest.SEARCH_FIELDS);
        Bson finalFilter = MongoFilter.and(baseFilter, searchFilter);
        Bson sort = MongoFilter.sort(request);

        PanacheQuery<TestScale> query = scaleService.find(finalFilter, sort).page(request.page() - 1,
                request.limit());
        long total = scaleService.count(finalFilter);

        List<TestScaleResponse> data = query.stream().map(TestScaleResponse::fromEntity).toList();
        return ResponseHelper.ok(data, "Scales retrieved successfully", PaginationMeta.of(request, total));
    }

    @GET
    @Path("/{testId}/scales/{scaleId}")
    @RolesAllowed({ "SUPERADMIN" })
    @Operation(summary = "Get scale detail")
    public Response getById(@PathParam("testId") ObjectId testId, @PathParam("scaleId") ObjectId scaleId) {
        TestScale scale = scaleService.getInTest(testId, scaleId);
        return ResponseHelper.ok(TestScaleResponse.fromEntity(scale), "Scale retrieved successfully");
    }

    @POST
    @Path("/{testId}/scales")
    @RolesAllowed({ "SUPERADMIN" })
    @Operation(summary = "Create new scale")
    public Response create(@PathParam("testId") ObjectId testId, @Valid TestScaleCreateRequest req) {
        TestScale scale = scaleService.create(
                testId, req.code(), req.name(), req.type(), req.role(),
                req.parentScaleId(), req.sectionId(), req.order(), req.description(),
                req.lowLabel(), req.highLabel(), req.rawMin(), req.rawMax(),
                req.scoreType(), req.composite(), req.isReported(), req.isValidityScale(), req.color());
        return ResponseHelper.created(TestScaleResponse.fromEntity(scale), "Scale berhasil dibuat");
    }

    @PUT
    @Path("/{testId}/scales/{scaleId}")
    @RolesAllowed({ "SUPERADMIN" })
    @Operation(summary = "Update existing scale")
    public Response update(@PathParam("testId") ObjectId testId, @PathParam("scaleId") ObjectId scaleId,
            @Valid TestScaleUpdateRequest req) {
        TestScale scale = scaleService.update(
                testId, scaleId, req.name(), req.type(), req.role(),
                req.parentScaleId(), req.sectionId(), req.order(), req.description(),
                req.lowLabel(), req.highLabel(), req.rawMin(), req.rawMax(),
                req.scoreType(), req.composite(), req.isReported(), req.isValidityScale(), req.color());
        return ResponseHelper.ok(TestScaleResponse.fromEntity(scale), "Scale berhasil diperbarui");
    }

    @DELETE
    @Path("/{testId}/scales/{scaleId}")
    @RolesAllowed({ "SUPERADMIN" })
    @Operation(summary = "Delete scale")
    public Response delete(@PathParam("testId") ObjectId testId, @PathParam("scaleId") ObjectId scaleId) {
        testCatalogService.deleteScale(testId, scaleId);
        return Response.noContent().build();
    }

    // =========================================================================
    // ITEMS
    // =========================================================================

    @GET
    @Path("/{testId}/items")
    @RolesAllowed({ "SUPERADMIN" })
    @Operation(summary = "Get all items for a test")
    public Response getAllItems(@PathParam("testId") ObjectId testId, @BeanParam TestItemListRequest request) {
        List<Bson> baseFilters = new ArrayList<>();
        baseFilters.add(Filters.eq("testId", testId));
        baseFilters.add(Filters.eq("deletedAt", null));
        if (request.sectionId() != null) {
            baseFilters.add(Filters.eq("sectionId", request.sectionId()));
        }
        if (request.scaleId() != null) {
            baseFilters.add(Filters.eq("scaleId", request.scaleId()));
        }
        if (request.type() != null && !request.type().isBlank()) {
            baseFilters.add(Filters.eq("type", request.type()));
        }

        Bson baseFilter = Filters.and(baseFilters);
        Bson searchFilter = MongoFilter.fromRequest(request, TestItemListRequest.SEARCH_FIELDS);
        Bson finalFilter = MongoFilter.and(baseFilter, searchFilter);
        Bson sort = MongoFilter.sort(request);

        int effectiveLimit = Math.min(request.limit(), 200);
        PanacheQuery<TestItem> query = itemService.find(finalFilter, sort).page(request.page() - 1, effectiveLimit);
        long total = itemService.count(finalFilter);

        List<TestItemResponse> data = query.stream().map(TestItemResponse::fromEntity).toList();
        return ResponseHelper.ok(data, "Items retrieved successfully", PaginationMeta.of(request, total));
    }

    @GET
    @Path("/{testId}/items/{itemId}")
    @RolesAllowed({ "SUPERADMIN" })
    @Operation(summary = "Get item detail")
    public Response getItemById(@PathParam("testId") ObjectId testId, @PathParam("itemId") ObjectId itemId) {
        TestItem item = itemService.getInTest(testId, itemId);
        return ResponseHelper.ok(TestItemResponse.fromEntity(item), "Item retrieved successfully");
    }

    @POST
    @Path("/{testId}/items")
    @RolesAllowed({ "SUPERADMIN" })
    @Operation(summary = "Create new item (soal)")
    public Response createItem(@PathParam("testId") ObjectId testId, @Valid TestItemCreateRequest req) {
        TestItem item = itemService.create(
                testId,
                req.sectionId(),
                req.number(),
                req.type(),
                req.content(),
                req.options(),
                req.groupId(),
                req.scaleId(),
                req.reverseScored(),
                req.weight(),
                req.scoredInTotal(),
                req.consistencyGroup(),
                req.answerKey(),
                req.timeLimitSeconds(),
                req.constraints(),
                req.difficulty(),
                req.isExample(),
                req.status(),
                req.tags());
        return ResponseHelper.created(TestItemResponse.fromEntity(item), "Soal berhasil dibuat");
    }

    @PUT
    @Path("/{testId}/items/{itemId}")
    @RolesAllowed({ "SUPERADMIN" })
    @Operation(summary = "Update existing item (soal)")
    public Response updateItem(@PathParam("testId") ObjectId testId, @PathParam("itemId") ObjectId itemId,
            @Valid TestItemUpdateRequest req) {
        TestItem item = itemService.update(
                testId,
                itemId,
                req.number(),
                req.content(),
                req.options(),
                req.groupId(),
                req.scaleId(),
                req.reverseScored(),
                req.weight(),
                req.scoredInTotal(),
                req.consistencyGroup(),
                req.answerKey(),
                req.timeLimitSeconds(),
                req.constraints(),
                req.difficulty(),
                req.isExample(),
                req.status(),
                req.tags());
        return ResponseHelper.ok(TestItemResponse.fromEntity(item), "Soal berhasil diperbarui");
    }

    @DELETE
    @Path("/{testId}/items/{itemId}")
    @RolesAllowed({ "SUPERADMIN" })
    @Operation(summary = "Delete item (soal)")
    public Response deleteItem(@PathParam("testId") ObjectId testId, @PathParam("itemId") ObjectId itemId) {
        itemService.delete(testId, itemId);
        return Response.noContent().build();
    }

    // =========================================================================
    // NORMS
    // =========================================================================

    @GET
    @Path("/{testId}/norms")
    @RolesAllowed({ "SUPERADMIN" })
    @Operation(summary = "Get all norms for a test")
    public Response getAllNorms(
            @PathParam("testId") ObjectId testId,
            @jakarta.ws.rs.QueryParam("scaleId") ObjectId scaleId,
            @jakarta.ws.rs.QueryParam("status") String status) {

        List<NormTable> norms = normService.listByTest(testId, scaleId, status);
        List<NormTableResponse> data = norms.stream()
                .map(n -> buildNormResponse(testId, n))
                .toList();
        return ResponseHelper.ok(data, "Norms retrieved successfully");
    }

    @POST
    @Path("/{testId}/norms")
    @RolesAllowed({ "SUPERADMIN" })
    @Operation(summary = "Create new norm")
    public Response createNorm(@PathParam("testId") ObjectId testId, @Valid NormCreateRequest req) {
        NormTable norm = normService.create(testId, req);
        return ResponseHelper.created(buildNormResponse(testId, norm), "Norma berhasil dibuat");
    }

    @PUT
    @Path("/{testId}/norms/{normId}")
    @RolesAllowed({ "SUPERADMIN" })
    @Operation(summary = "Update norm (DRAFT only)")
    public Response updateNorm(@PathParam("testId") ObjectId testId, @PathParam("normId") ObjectId normId,
            @Valid NormUpdateRequest req) {
        NormTable norm = normService.update(testId, normId, req);
        return ResponseHelper.ok(buildNormResponse(testId, norm), "Norma berhasil diperbarui");
    }

    @DELETE
    @Path("/{testId}/norms/{normId}")
    @RolesAllowed({ "SUPERADMIN" })
    @Operation(summary = "Delete norm (DRAFT only)")
    public Response deleteNorm(@PathParam("testId") ObjectId testId, @PathParam("normId") ObjectId normId) {
        normService.delete(testId, normId);
        return Response.noContent().build();
    }

    @POST
    @Path("/{testId}/norms/{normId}/activate")
    @RolesAllowed({ "SUPERADMIN" })
    @Operation(summary = "Activate norm (DRAFT to ACTIVE)")
    public Response activateNorm(@PathParam("testId") ObjectId testId, @PathParam("normId") ObjectId normId) {
        NormTable norm = normService.activate(testId, normId);
        return ResponseHelper.ok(buildNormResponse(testId, norm), "Norma berhasil diaktifkan");
    }

    @POST
    @Path("/{testId}/norms/{normId}/archive")
    @RolesAllowed({ "SUPERADMIN" })
    @Operation(summary = "Archive norm (ACTIVE to ARCHIVED)")
    public Response archiveNorm(@PathParam("testId") ObjectId testId, @PathParam("normId") ObjectId normId) {
        NormTable norm = normService.archive(testId, normId);
        return ResponseHelper.ok(buildNormResponse(testId, norm), "Norma berhasil diarsipkan");
    }

    private NormTableResponse buildNormResponse(ObjectId testId, NormTable norm) {
        var scale = scaleService.findInTest(testId, norm.getScaleId());
        Map<String, Object> coverage = null;
        if (norm.getMethod() == com.psycorp.psychapi.feature.test.models.NormTable.NormMethod.LOOKUP
                && scale != null && scale.getRawMin() != null && scale.getRawMax() != null) {
            coverage = normService.computeCoverage(norm.getEntries(), scale.getRawMin(), scale.getRawMax());
        }
        return NormTableResponse.fromEntity(norm, scale, coverage);
    }
}