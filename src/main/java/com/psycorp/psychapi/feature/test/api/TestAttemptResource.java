package com.psycorp.psychapi.feature.test.api;

import java.util.List;
import java.util.Map;

import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.mongodb.client.model.Filters;
import com.psycorp.psychapi.feature.test.api.dto.request.AttemptListRequest;
import com.psycorp.psychapi.feature.test.api.dto.request.SaveAnswersRequest;
import com.psycorp.psychapi.feature.test.api.dto.request.StartAttemptRequest;
import com.psycorp.psychapi.feature.test.api.dto.request.SubmitSectionRequest;
import com.psycorp.psychapi.feature.test.api.dto.request.TestDefinitionListRequest;
import com.psycorp.psychapi.feature.test.api.dto.response.AttemptResponse;
import com.psycorp.psychapi.feature.test.api.dto.response.SectionStartResponse;
import com.psycorp.psychapi.feature.test.api.dto.response.TestCatalogResponse;
import com.psycorp.psychapi.feature.test.api.dto.response.TestDetailPublicResponse;
import com.psycorp.psychapi.feature.test.models.TestAttempt;
import com.psycorp.psychapi.feature.test.models.TestDefinition;
import com.psycorp.psychapi.feature.test.models.TestSection;
import com.psycorp.psychapi.feature.test.service.TestAttemptService;
import com.psycorp.psychapi.feature.test.service.TestDefinitionService;
import com.psycorp.psychapi.feature.test.service.TestSectionService;
import com.psycorp.psychapi.feature.user.models.User;
import com.psycorp.psychapi.shared.response.PaginationMeta;
import com.psycorp.psychapi.shared.response.ResponseHelper;
import com.psycorp.psychapi.shared.util.MongoFilter;

import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.security.ForbiddenException;
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
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/tests")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Test (Peserta)", description = "API proses pengerjaan tes untuk peserta")
public class TestAttemptResource {

  @Inject
  TestDefinitionService testDefinitionService;

  @Inject
  TestSectionService sectionService;

  @Inject
  TestAttemptService attemptService;

  // =====================================================================
  // KATALOG TES
  // =====================================================================
  @GET
  @Path("/catalog")
  @Operation(summary = "Katalog tes yang tersedia (ACTIVE)")
  public Response getCatalog(@BeanParam TestDefinitionListRequest request) {
    Bson baseFilter = Filters.and(Filters.eq("status", "ACTIVE"), Filters.eq("deletedAt", null));
    Bson searchFilter = MongoFilter.fromRequest(request, TestDefinitionListRequest.SEARCH_FIELDS);
    Bson finalFilter = MongoFilter.and(baseFilter, searchFilter);
    Bson sort = MongoFilter.sort(request);

    PanacheQuery<TestDefinition> query = testDefinitionService.find(finalFilter, sort)
        .page(request.page() - 1, request.limit());
    long total = testDefinitionService.count(finalFilter);

    List<TestCatalogResponse> data = query.stream().map(TestCatalogResponse::fromEntity).toList();
    return ResponseHelper.ok(data, "Katalog tes berhasil diambil", PaginationMeta.of(request, total));
  }

  @GET
  @Path("/catalog/{testId}")
  @Operation(summary = "Detail tes dan daftar subtes")
  @RolesAllowed("USER")
  public Response getTestDetail(
      @PathParam("testId") ObjectId testId, @Context ContainerRequestContext requestContext) {

    User user = (User) requestContext.getProperty("validatedUser");
    if (user == null)
      throw new ForbiddenException("Authentication required");

    TestDefinition test = testDefinitionService.findById(testId);
    if (test == null || test.getDeletedAt() != null
        || test.getStatus() != TestDefinition.TestStatus.ACTIVE) {
      return ResponseHelper.notFound("Tes tidak ditemukan", "/api/v1/tests/" + testId);
    }

    List<TestSection> sections = sectionService.listByTest(testId);

    // Cari attempt IN_PROGRESS milik user ini
    Map<String, Object> myAttempt = null;
    TestAttempt active = attemptService.find(
        "userId = ?1 and testId = ?2 and status = 'IN_PROGRESS'",
        user.id, testId).firstResult();
    if (active != null) {
      myAttempt = Map.of("id", active.id.toHexString(), "status", active.getStatus().getValue());
    }

    return ResponseHelper.ok(
        TestDetailPublicResponse.fromEntity(test, sections, myAttempt),
        "Detail tes berhasil diambil");
  }

  // =====================================================================
  // ATTEMPT
  // =====================================================================

  @POST
  @Path("/attempts")
  @Operation(summary = "Mulai atau lanjutkan attempt")
  public Response startOrResume(
      @Context ContainerRequestContext requestContext,
      @Valid StartAttemptRequest req) {

    User user = (User) requestContext.getProperty("validatedUser");
    if (user == null)
      throw new ForbiddenException("Authentication required");

    boolean isNew = attemptService.find(
        "userId = ?1 and testId = ?2 and status = 'IN_PROGRESS'",
        user.id, req.testId()).firstResult() == null;

    TestAttempt attempt = attemptService.startOrResume(user.id, req.testId());
    List<Map<String, Object>> sections = attemptService.buildSectionsWithCount(attempt);
    AttemptResponse resp = AttemptResponse.fromEntity(attempt, sections);

    return isNew
        ? ResponseHelper.created(resp, "Attempt berhasil dimulai")
        : ResponseHelper.ok(resp, "Attempt dilanjutkan");
  }

  @GET
  @Path("/attempts")
  @RolesAllowed("USER")
  @Operation(summary = "Riwayat attempt milik user")
  public Response listAttempts(
      @Context ContainerRequestContext requestContext,
      @BeanParam AttemptListRequest request) {

    User user = (User) requestContext.getProperty("validatedUser");

    // userId selalu dipaksakan — user hanya bisa lihat attempt miliknya sendiri
    Bson baseFilter = Filters.eq("userId", user.id);
    if (request.status() != null && !request.status().isBlank())
      baseFilter = Filters.and(baseFilter, Filters.eq("status", request.status()));
    if (request.testId() != null)
      baseFilter = Filters.and(baseFilter, Filters.eq("testId", request.testId()));

    Bson sort = MongoFilter.sort(request);

    PanacheQuery<TestAttempt> query = attemptService.find(baseFilter, sort)
        .page(request.page() - 1, request.limit());
    long total = attemptService.count(baseFilter);

    List<Map<String, Object>> data = query.stream().map(a -> {
      long answered = a.getSectionStates() != null
          ? a.getSectionStates().stream()
              .mapToLong(s -> attemptService.countAnsweredInSection(
                  a.id, new ObjectId(String.valueOf(s.get("sectionId")))))
              .sum()
          : 0;
      Map<String, Object> row = new java.util.HashMap<>();
      row.put("id", a.id.toHexString());
      row.put("testId", a.getTestId() != null ? a.getTestId().toHexString() : "");
      row.put("testCode", a.getTestCode() != null ? a.getTestCode() : "");
      row.put("testName", a.getTestName() != null ? a.getTestName() : "");
      row.put("attemptNumber", a.getAttemptNumber());
      row.put("status", a.getStatus().getValue());
      row.put("startedAt", a.getStartedAt() != null ? a.getStartedAt().toString() : null);
      row.put("completedAt", a.getCompletedAt() != null ? a.getCompletedAt().toString() : null);
      row.put("answeredItems", answered);
      row.put("totalItems", a.getSectionStates() != null
          ? a.getSectionStates().stream()
              .mapToLong(s -> s.get("itemCount") instanceof Integer i ? i : 0).sum()
          : 0);
      return row;
    }).toList();

    return ResponseHelper.ok(data, "Riwayat attempt berhasil diambil", PaginationMeta.of(request, total));
  }

  @GET
  @Path("/attempts/{attemptId}")
  @Operation(summary = "Status attempt dan sisa waktu")
  public Response getAttempt(
      @Context ContainerRequestContext requestContext,
      @PathParam("attemptId") ObjectId attemptId) {

    User user = (User) requestContext.getProperty("validatedUser");
    if (user == null)
      throw new ForbiddenException("Authentication required");

    TestAttempt attempt = attemptService.getOwned(attemptId, user.id);
    List<Map<String, Object>> sections = attemptService.buildSectionsWithCount(attempt);
    return ResponseHelper.ok(AttemptResponse.fromEntity(attempt, sections), "Attempt berhasil diambil");
  }

  // =====================================================================
  // SECTION
  // =====================================================================

  @POST
  @Path("/attempts/{attemptId}/sections/{sectionId}/start")
  @Operation(summary = "Mulai subtes — timer berjalan, soal dan jawaban dikirim")
  public Response startSection(
      @Context ContainerRequestContext requestContext,
      @PathParam("attemptId") ObjectId attemptId,
      @PathParam("sectionId") ObjectId sectionId) {

    User user = (User) requestContext.getProperty("validatedUser");
    if (user == null)
      throw new ForbiddenException("Authentication required");

    Map<String, Object> result = attemptService.startSection(attemptId, user.id, sectionId);

    @SuppressWarnings("unchecked")
    Map<String, Object> sectionInfo = (Map<String, Object>) result.get("section");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> answers = (List<Map<String, Object>>) result.get("answers");

    SectionStartResponse.SectionInfo si = new SectionStartResponse.SectionInfo(
        String.valueOf(sectionInfo.get("sectionId")),
        String.valueOf(sectionInfo.get("code")),
        String.valueOf(sectionInfo.get("name")),
        null, // instructions tidak disimpan di sectionStates — ambil dari TestSection
        null, null,
        sectionInfo.get("timeLimitSeconds") instanceof Integer i ? i : null,
        String.valueOf(sectionInfo.get("navigationPolicy")),
        sectionInfo.get("itemCount") instanceof Integer ic ? ic : null,
        sectionInfo.get("startedAt") != null
            ? java.time.Instant.parse(sectionInfo.get("startedAt").toString())
            : null,
        sectionInfo.get("deadlineAt") != null
            ? java.time.Instant.parse(sectionInfo.get("deadlineAt").toString())
            : null,
        java.time.Instant.now());

    // Ambil instructions dan examples dari TestSection
    TestSection section = sectionService.findInTest(
        attemptService.getOwned(attemptId, user.id).getTestId(), sectionId);
    SectionStartResponse.SectionInfo full = section != null
        ? new SectionStartResponse.SectionInfo(
            si.id(), si.code(), si.name(),
            section.getInstructions(),
            section.getExamples(),
            section.getShowExamplesFirst(),
            si.timeLimitSeconds(), si.navigationPolicy(), si.itemCount(),
            si.startedAt(), si.deadlineAt(), si.serverTime())
        : si;

    return ResponseHelper.ok(new SectionStartResponse(full, items, answers), "Subtes berhasil dimulai");
  }

  @PUT
  @Path("/attempts/{attemptId}/sections/{sectionId}/answers")
  @Operation(summary = "Simpan jawaban (autosave)")
  public Response saveAnswers(
      @Context ContainerRequestContext requestContext,
      @PathParam("attemptId") ObjectId attemptId,
      @PathParam("sectionId") ObjectId sectionId,
      @Valid SaveAnswersRequest req) {

    User user = (User) requestContext.getProperty("validatedUser");
    if (user == null)
      throw new ForbiddenException("Authentication required");

    Map<String, Object> result = attemptService.saveAnswers(
        attemptId, user.id, sectionId, req.answers(), req.revision());
    return ResponseHelper.ok(result, "Jawaban berhasil disimpan");
  }

  @POST
  @Path("/attempts/{attemptId}/sections/{sectionId}/submit")
  @Operation(summary = "Selesaikan subtes")
  public Response submitSection(
      @Context ContainerRequestContext requestContext,
      @PathParam("attemptId") ObjectId attemptId,
      @PathParam("sectionId") ObjectId sectionId,
      SubmitSectionRequest req) {

    User user = (User) requestContext.getProperty("validatedUser");
    if (user == null)
      throw new ForbiddenException("Authentication required");

    List<Map<String, Object>> finalAnswers = req != null ? req.answers() : null;
    Map<String, Object> result = attemptService.submitSection(
        attemptId, user.id, sectionId, finalAnswers);
    return ResponseHelper.ok(result, "Subtes berhasil diselesaikan");
  }

  @POST
  @Path("/attempts/{attemptId}/finish")
  @Operation(summary = "Selesaikan tes")
  public Response finish(
      @Context ContainerRequestContext requestContext,
      @PathParam("attemptId") ObjectId attemptId) {

    User user = (User) requestContext.getProperty("validatedUser");
    if (user == null)
      throw new ForbiddenException("Authentication required");

    TestAttempt attempt = attemptService.finish(attemptId, user.id);
    return ResponseHelper.ok(Map.of(
        "id", attempt.id.toHexString(),
        "status", attempt.getStatus().getValue(),
        "completedAt", attempt.getCompletedAt().toString(),
        "scoringStatus", attempt.getScoringStatus().getValue()),
        "Tes berhasil diselesaikan");
  }
}
