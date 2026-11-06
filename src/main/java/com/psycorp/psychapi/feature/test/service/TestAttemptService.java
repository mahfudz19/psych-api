package com.psycorp.psychapi.feature.test.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

import com.psycorp.psychapi.feature.test.models.AttemptAnswer.AnswerStatus;
import com.psycorp.psychapi.feature.test.models.TestAttempt;
import com.psycorp.psychapi.feature.test.models.TestAttempt.AttemptStatus;
import com.psycorp.psychapi.feature.test.models.TestDefinition;
import com.psycorp.psychapi.feature.test.models.TestDefinition.TestStatus;
import com.psycorp.psychapi.feature.test.models.TestItem;
import com.psycorp.psychapi.feature.test.models.TestSection;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TestAttemptService implements PanacheMongoRepository<TestAttempt> {

    @Inject
    TestDefinitionService tests;

    @Inject
    TestSectionService sections;

    @Inject
    TestItemService items;

    @Inject
    AttemptAnswerService answerService;

    // =====================================================================
    // MULAI / LANJUTKAN ATTEMPT
    // =====================================================================

    /**
     * Mengembalikan attempt IN_PROGRESS yang sudah ada, atau membuat baru.
     * Lazy close section expired dijalankan sebelum dikembalikan.
     */
    public TestAttempt startOrResume(ObjectId userId, ObjectId testId) {
        TestDefinition test = tests.findById(testId);
        if (test == null || test.getDeletedAt() != null) {
            throw new ValidationException("TEST_NOT_FOUND", "Tes tidak ditemukan");
        }
        if (test.getStatus() != TestStatus.ACTIVE) {
            throw new ValidationException("TEST_NOT_ACTIVE", "Tes belum tersedia untuk dikerjakan");
        }

        // Kembalikan attempt yang sudah ada jika IN_PROGRESS
        TestAttempt existing = find("userId = ?1 and testId = ?2 and status = 'IN_PROGRESS'",
                userId, testId).firstResult();
        if (existing != null) {
            lazyCloseExpired(existing);
            return existing;
        }

        // Hitung attemptNumber
        long prev = count("userId = ?1 and testId = ?2", userId, testId);

        List<TestSection> testSections = sections.listByTest(testId);
        List<Map<String, Object>> sectionStates = buildSectionStates(testSections);

        Instant now = Instant.now();
        TestAttempt attempt = new TestAttempt();
        attempt.setUserId(userId);
        attempt.setTestId(testId);
        attempt.setTestVersion(test.getVersion());
        attempt.setTestCode(test.getCode());
        attempt.setTestName(test.getName());
        attempt.setAttemptNumber((int) prev + 1);
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attempt.setSectionStates(sectionStates);
        attempt.setStartedAt(now);
        attempt.setLastActivityAt(now);
        attempt.setCreatedAt(now);
        attempt.setUpdatedAt(now);

        persist(attempt);
        return attempt;
    }

    // =====================================================================
    // QUERY
    // =====================================================================

    public TestAttempt getOwned(ObjectId attemptId, ObjectId userId) {
        TestAttempt attempt = findById(attemptId);
        if (attempt == null || !userId.equals(attempt.getUserId())) {
            throw new ValidationException("ATTEMPT_NOT_FOUND", "Attempt tidak ditemukan");
        }
        return attempt;
    }

    public List<TestAttempt> listByUser(ObjectId userId, String status, ObjectId testId) {
        if (status != null && testId != null) {
            return find("userId = ?1 and status = ?2 and testId = ?3",
                    Sort.descending("startedAt"), userId, status, testId).list();
        }
        if (status != null) {
            return find("userId = ?1 and status = ?2",
                    Sort.descending("startedAt"), userId, status).list();
        }
        if (testId != null) {
            return find("userId = ?1 and testId = ?2",
                    Sort.descending("startedAt"), userId, testId).list();
        }
        return find("userId = ?1", Sort.descending("startedAt"), userId).list();
    }

    // =====================================================================
    // START SECTION
    // =====================================================================

    /**
     * Mulai section. Idempoten: kalau sudah IN_PROGRESS, kembalikan state yang ada.
     * Mengembalikan map berisi section info + items (tanpa kunci) + answers tersimpan.
     */
    public Map<String, Object> startSection(ObjectId attemptId, ObjectId userId, ObjectId sectionId) {
        TestAttempt attempt = getOwned(attemptId, userId);
        requireInProgress(attempt);
        lazyCloseExpired(attempt);

        Map<String, Object> state = getSectionState(attempt, sectionId);
        if (state == null) {
            throw new ValidationException("SECTION_NOT_FOUND", "Section tidak ada di attempt ini");
        }

        String sectionStatus = (String) state.get("status");

        if ("COMPLETED".equals(sectionStatus) || "EXPIRED".equals(sectionStatus)) {
            throw new ValidationException("INVALID_STATE", "Section sudah selesai dan tidak bisa dibuka lagi");
        }

        // Pastikan tidak ada section lain yang IN_PROGRESS
        if (!"IN_PROGRESS".equals(sectionStatus)) {
            boolean anotherInProgress = attempt.getSectionStates().stream()
                    .anyMatch(s -> "IN_PROGRESS".equals(s.get("status"))
                            && !sectionId.toHexString().equals(String.valueOf(s.get("sectionId"))));
            if (anotherInProgress) {
                throw new ValidationException("INVALID_STATE",
                        "Selesaikan subtes yang sedang berjalan terlebih dahulu");
            }

            // Tandai mulai
            Instant now = Instant.now();
            state.put("status", "IN_PROGRESS");
            state.put("startedAt", now.toString());

            Integer timeLimitSeconds = (Integer) state.get("timeLimitSeconds");
            if (timeLimitSeconds != null) {
                state.put("deadlineAt", now.plusSeconds(timeLimitSeconds).toString());
            }

            attempt.setCurrentSectionId(sectionId);
            attempt.setLastActivityAt(now);
            attempt.setUpdatedAt(now);
            update(attempt);

            // Buat dokumen jawaban
            answerService.create(attemptId, sectionId, attempt.getTestId(), userId);
        }

        // Ambil soal (tanpa kunci)
        List<Map<String, Object>> itemDtos = getItemsForSection(attempt.getTestId(), sectionId);

        // Ambil jawaban tersimpan
        var answerDoc = answerService.findBySection(attemptId, sectionId);
        List<Map<String, Object>> savedAnswers = answerDoc != null && answerDoc.getAnswers() != null
                ? answerDoc.getAnswers() : List.of();

        // Bangun section info
        Map<String, Object> sectionInfo = new HashMap<>(state);
        sectionInfo.put("serverTime", Instant.now().toString());

        Map<String, Object> result = new HashMap<>();
        result.put("section", sectionInfo);
        result.put("items", itemDtos);
        result.put("answers", savedAnswers);
        return result;
    }

    // =====================================================================
    // SIMPAN JAWABAN
    // =====================================================================

    public Map<String, Object> saveAnswers(ObjectId attemptId, ObjectId userId, ObjectId sectionId,
            List<Map<String, Object>> answers, Integer clientRevision) {
        TestAttempt attempt = getOwned(attemptId, userId);
        requireInProgress(attempt);
        lazyCloseExpired(attempt);

        Map<String, Object> state = getSectionState(attempt, sectionId);
        if (state == null || !"IN_PROGRESS".equals(state.get("status"))) {
            throw new ValidationException("INVALID_STATE", "Section tidak sedang IN_PROGRESS");
        }

        // Cek deadline (toleransi 5 detik)
        String deadlineStr = (String) state.get("deadlineAt");
        if (deadlineStr != null) {
            Instant deadline = Instant.parse(deadlineStr);
            if (Instant.now().isAfter(deadline.plusSeconds(5))) {
                throw new ValidationException("SECTION_EXPIRED", "Waktu subtes sudah habis");
            }
        }

        var answerDoc = answerService.findBySection(attemptId, sectionId);
        if (answerDoc == null) {
            throw new ValidationException("INVALID_STATE", "Dokumen jawaban belum ada, mulai section terlebih dahulu");
        }

        int savedCount = answerService.upsertAnswers(answerDoc, answers);

        attempt.setLastActivityAt(Instant.now());
        attempt.setUpdatedAt(Instant.now());
        update(attempt);

        return Map.of(
                "savedCount", savedCount,
                "revision", answerDoc.getRevision(),
                "deadlineAt", deadlineStr != null ? deadlineStr : null,
                "serverTime", Instant.now().toString());
    }

    // =====================================================================
    // SUBMIT SECTION
    // =====================================================================

    public Map<String, Object> submitSection(ObjectId attemptId, ObjectId userId, ObjectId sectionId,
            List<Map<String, Object>> finalAnswers) {
        TestAttempt attempt = getOwned(attemptId, userId);
        requireInProgress(attempt);
        lazyCloseExpired(attempt);

        Map<String, Object> state = getSectionState(attempt, sectionId);
        if (state == null || !"IN_PROGRESS".equals(state.get("status"))) {
            throw new ValidationException("INVALID_STATE", "Section tidak sedang IN_PROGRESS");
        }

        // Simpan jawaban terakhir jika ada
        if (finalAnswers != null && !finalAnswers.isEmpty()) {
            var answerDoc = answerService.findBySection(attemptId, sectionId);
            if (answerDoc != null) {
                answerService.upsertAnswers(answerDoc, finalAnswers);
                answerService.close(answerDoc, AnswerStatus.SUBMITTED);
            }
        } else {
            var answerDoc = answerService.findBySection(attemptId, sectionId);
            if (answerDoc != null) answerService.close(answerDoc, AnswerStatus.SUBMITTED);
        }

        // Tandai section COMPLETED
        Instant now = Instant.now();
        state.put("status", "COMPLETED");
        state.put("completedAt", now.toString());

        long answeredCount = answerService.countAnswered(attemptId, sectionId);

        // Cari section berikutnya
        Integer currentOrder = (Integer) state.get("order");
        String nextSectionId = attempt.getSectionStates().stream()
                .filter(s -> {
                    Object o = s.get("order");
                    return o instanceof Integer i && i.equals(currentOrder + 1);
                })
                .map(s -> String.valueOf(s.get("sectionId")))
                .findFirst().orElse(null);

        attempt.setCurrentSectionId(nextSectionId != null ? new ObjectId(nextSectionId) : null);
        attempt.setLastActivityAt(now);
        attempt.setUpdatedAt(now);
        update(attempt);

        return Map.of(
                "sectionId", sectionId.toHexString(),
                "status", "COMPLETED",
                "answeredCount", answeredCount,
                "itemCount", state.getOrDefault("itemCount", 0),
                "nextSectionId", nextSectionId != null ? nextSectionId : "",
                "attemptStatus", attempt.getStatus().getValue(),
                "serverTime", now.toString());
    }

    // =====================================================================
    // FINISH
    // =====================================================================

    public TestAttempt finish(ObjectId attemptId, ObjectId userId) {
        TestAttempt attempt = getOwned(attemptId, userId);
        requireInProgress(attempt);
        lazyCloseExpired(attempt);

        boolean allDone = attempt.getSectionStates().stream()
                .allMatch(s -> "COMPLETED".equals(s.get("status")) || "EXPIRED".equals(s.get("status")));
        if (!allDone) {
            throw new ValidationException("INVALID_STATE",
                    "Semua subtes harus selesai atau expired sebelum tes bisa diakhiri");
        }

        Instant now = Instant.now();
        attempt.setStatus(AttemptStatus.COMPLETED);
        attempt.setCompletedAt(now);
        attempt.setScoringStatus(TestAttempt.ScoringStatus.PENDING);
        attempt.setUpdatedAt(now);
        update(attempt);
        return attempt;
    }

    // =====================================================================
    // DIPAKAI RESOURCE — bangun sections dengan answeredCount
    // =====================================================================

    public long countAnsweredInSection(ObjectId attemptId, ObjectId sectionId) {
        return answerService.countAnswered(attemptId, sectionId);
    }

    public List<Map<String, Object>> buildSectionsWithCount(TestAttempt attempt) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> state : attempt.getSectionStates()) {
            Map<String, Object> row = new HashMap<>(state);
            Object rawSectionId = state.get("sectionId");
            if (rawSectionId != null) {
                ObjectId sid = new ObjectId(rawSectionId.toString());
                long answeredCount = answerService.countAnswered(attempt.id, sid);
                row.put("answeredCount", answeredCount);
            }
            result.add(row);
        }
        return result;
    }

    // =====================================================================
    // PRIVATE
    // =====================================================================

    /** Menutup semua section yang deadlineAt-nya sudah lewat. */
    private void lazyCloseExpired(TestAttempt attempt) {
        if (attempt.getSectionStates() == null) return;
        Instant now = Instant.now();
        boolean changed = false;
        List<ObjectId> expiredSections = new ArrayList<>();

        for (Map<String, Object> state : attempt.getSectionStates()) {
            if (!"IN_PROGRESS".equals(state.get("status"))) continue;
            String deadlineStr = (String) state.get("deadlineAt");
            if (deadlineStr == null) continue;
            if (now.isAfter(Instant.parse(deadlineStr))) {
                state.put("status", "EXPIRED");
                state.put("completedAt", now.toString());
                Object rawId = state.get("sectionId");
                if (rawId != null) expiredSections.add(new ObjectId(rawId.toString()));
                changed = true;
            }
        }

        if (changed) {
            answerService.autoCloseExpired(attempt.id, expiredSections);
            attempt.setUpdatedAt(now);
            update(attempt);
        }
    }

    private List<Map<String, Object>> buildSectionStates(List<TestSection> testSections) {
        return testSections.stream().map(s -> {
            Map<String, Object> state = new HashMap<>();
            state.put("sectionId", s.getId().toHexString());
            state.put("code", s.getCode());
            state.put("order", s.getOrder());
            state.put("name", s.getName());
            state.put("itemCount", s.getItemCount());
            state.put("timeLimitSeconds", s.getTimeLimitSeconds());
            state.put("navigationPolicy", s.getNavigationPolicy() != null
                    ? s.getNavigationPolicy().getValue() : "FREE");
            state.put("status", "NOT_STARTED");
            state.put("startedAt", null);
            state.put("deadlineAt", null);
            state.put("completedAt", null);
            return state;
        }).toList();
    }

    private Map<String, Object> getSectionState(TestAttempt attempt, ObjectId sectionId) {
        if (attempt.getSectionStates() == null) return null;
        return attempt.getSectionStates().stream()
                .filter(s -> sectionId.toHexString().equals(String.valueOf(s.get("sectionId"))))
                .findFirst().orElse(null);
    }

    private List<Map<String, Object>> getItemsForSection(ObjectId testId, ObjectId sectionId) {
        List<TestItem> rawItems = items.listActiveBySection(testId, sectionId);
        return rawItems.stream().map(item -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", item.getId().toHexString());
            dto.put("number", item.getNumber());
            dto.put("type", item.getType() != null ? item.getType().getValue() : null);
            dto.put("content", item.getContent());
            dto.put("constraints", item.getConstraints());
            dto.put("groupId", item.getGroupId());
            dto.put("timeLimitSeconds", item.getTimeLimitSeconds());
            // Saring options — buang scoring, kirim hanya yang dibutuhkan peserta
            if (item.getOptions() != null) {
                List<Map<String, Object>> publicOptions = item.getOptions().stream().map(opt -> {
                    Map<String, Object> pub = new HashMap<>();
                    pub.put("key", opt.get("key"));
                    pub.put("label", opt.get("label"));
                    pub.put("imageUrl", opt.get("imageUrl"));
                    pub.put("order", opt.get("order"));
                    return pub;
                }).toList();
                dto.put("options", publicOptions);
            } else {
                dto.put("options", List.of());
            }
            return dto;
        }).toList();
    }

    private void requireInProgress(TestAttempt attempt) {
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new ValidationException("INVALID_STATE", "Attempt sudah selesai atau tidak aktif");
        }
    }
}
