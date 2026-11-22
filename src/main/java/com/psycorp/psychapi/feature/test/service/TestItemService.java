package com.psycorp.psychapi.feature.test.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import com.psycorp.psychapi.feature.storage.service.FileCleanupEvent;
import com.psycorp.psychapi.feature.storage.service.StorageService;
import com.psycorp.psychapi.feature.test.models.TestDefinition;
import com.psycorp.psychapi.feature.test.models.TestItem;
import com.psycorp.psychapi.feature.test.models.TestItem.ItemStatus;
import com.psycorp.psychapi.feature.test.models.TestItem.ItemType;
import com.psycorp.psychapi.feature.test.models.TestSection;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.InternalServerErrorException;

@ApplicationScoped
public class TestItemService implements PanacheMongoRepository<TestItem> {

    @Inject
    TestDefinitionService tests;

    @Inject
    TestSectionService sections;

    @Inject
    TestScaleService scales;

    @Inject
    StorageService storageService;

    @Inject
    Event<FileCleanupEvent> fileCleanupEvent;

    // =====================================================================
    // CRUD
    // =====================================================================

    public TestItem create(
            ObjectId testId,
            ObjectId sectionId,
            Integer number,
            ItemType type,
            Map<String, Object> content,
            List<Map<String, Object>> options,
            String groupId,
            ObjectId scaleId,
            Boolean reverseScored,
            Double weight,
            Boolean scoredInTotal,
            String consistencyGroup,
            Map<String, Object> answerKey,
            Integer timeLimitSeconds,
            Map<String, Object> constraints,
            Double difficulty,
            Boolean isExample,
            ItemStatus status,
            List<String> tags) {

        TestDefinition test = tests.requireDraft(testId);

        // Validasi section
        if (sections.findInTest(testId, sectionId) == null) {
            throw new ValidationException("SECTION_NOT_FOUND", "Section tidak ditemukan di tes ini");
        }

        // Validasi number unik per section
        if (numberExists(testId, sectionId, number, null)) {
            throw new ValidationException("DUPLICATE_NUMBER",
                    "Nomor soal " + number + " sudah dipakai di section ini");
        }

        // Validasi content
        if (content == null) {
            throw new ValidationException("VALIDATION_FAILED", "content wajib diisi");
        }

        // Validasi scaleId
        if (scaleId != null && scales.findInTest(testId, scaleId) == null) {
            throw new ValidationException("REFERENCE_NOT_FOUND", "Scale tidak ditemukan di tes ini");
        }

        // Validasi scaleMappings di options
        validateScaleMappings(testId, options);

        // Validasi aturan per tipe soal
        validateItemRules(type, options, reverseScored, scaleId, weight, difficulty,
                timeLimitSeconds);

        // Commit temp files ke path permanen sebelum simpan DB
        List<String> committedUrls = commitTempFiles(content, options);

        TestItem item = new TestItem();
        item.setTestId(testId);
        item.setSectionId(sectionId);
        item.setTestVersion(test.getVersion());
        item.setType(type);

        apply(item, number, content, options, groupId, scaleId, reverseScored, weight, scoredInTotal,
                consistencyGroup, answerKey, timeLimitSeconds, constraints, difficulty, isExample, status, tags);

        Instant now = Instant.now();
        item.setCreatedAt(now);
        item.setUpdatedAt(now);

        try {
            persist(item);
        } catch (Exception e) {
            rollbackCommittedFiles(committedUrls);
            throw new InternalServerErrorException("Gagal menyimpan soal", e);
        }
        syncCounts(testId, sectionId);
        return item;
    }

    public TestItem update(
            ObjectId testId,
            ObjectId itemId,
            Integer number,
            Map<String, Object> content,
            List<Map<String, Object>> options,
            String groupId,
            ObjectId scaleId,
            Boolean reverseScored,
            Double weight,
            Boolean scoredInTotal,
            String consistencyGroup,
            Map<String, Object> answerKey,
            Integer timeLimitSeconds,
            Map<String, Object> constraints,
            Double difficulty,
            Boolean isExample,
            ItemStatus status,
            List<String> tags) {

        tests.requireDraft(testId);
        TestItem item = getInTest(testId, itemId);

        // Validasi number unik per section (kecuali soal ini sendiri)
        if (numberExists(testId, item.getSectionId(), number, itemId)) {
            throw new ValidationException("DUPLICATE_NUMBER",
                    "Nomor soal " + number + " sudah dipakai di section ini");
        }

        // Validasi content
        if (content == null) {
            throw new ValidationException("VALIDATION_FAILED", "content wajib diisi");
        }

        // Validasi scaleId
        if (scaleId != null && scales.findInTest(testId, scaleId) == null) {
            throw new ValidationException("REFERENCE_NOT_FOUND", "Scale tidak ditemukan di tes ini");
        }

        // Validasi scaleMappings di options
        validateScaleMappings(testId, options);

        // Validasi aturan per tipe soal (type tidak bisa diubah, pakai type existing)
        validateItemRules(item.getType(), options, reverseScored, scaleId, weight,
                difficulty, timeLimitSeconds);

        // Kumpulkan file URL lama sebelum di-overwrite
        Set<String> oldUrls = extractAllFileUrls(item.getContent(), item.getOptions());

        // Commit temp files baru
        List<String> committedUrls = commitTempFiles(content, options);

        apply(item, number, content, options, groupId, scaleId, reverseScored, weight, scoredInTotal,
                consistencyGroup, answerKey, timeLimitSeconds, constraints, difficulty, isExample, status, tags);
        item.setUpdatedAt(Instant.now());

        try {
            update(item);
        } catch (Exception e) {
            rollbackCommittedFiles(committedUrls);
            throw new InternalServerErrorException("Gagal menyimpan soal", e);
        }
        syncCounts(testId, item.getSectionId());

        // Hapus file lama yang sudah tidak dipakai (async)
        Set<String> newUrls = extractAllFileUrls(content, options);
        oldUrls.removeAll(newUrls);
        if (!oldUrls.isEmpty()) {
            fileCleanupEvent.fireAsync(new FileCleanupEvent(List.copyOf(oldUrls)));
        }

        return item;
    }

    public void delete(ObjectId testId, ObjectId itemId) {
        tests.requireDraft(testId);
        TestItem item = getInTest(testId, itemId);

        // Kumpulkan file URLs sebelum soft-delete
        Set<String> fileUrls = extractAllFileUrls(item.getContent(), item.getOptions());

        item.setDeletedAt(Instant.now());
        update(item);
        syncCounts(testId, item.getSectionId());

        // Hapus file di cloud (async)
        if (!fileUrls.isEmpty()) {
            fileCleanupEvent.fireAsync(new FileCleanupEvent(List.copyOf(fileUrls)));
        }
    }

    public void softDeleteAllByTest(ObjectId testId) {
        update("deletedAt = ?1 where testId = ?2 and deletedAt is null", Instant.now(), testId);
    }

    // =====================================================================
    // LOOKUP
    // =====================================================================

    public TestItem findInTest(ObjectId testId, ObjectId itemId) {
        return find("{'_id': ?1, 'testId': ?2, 'deletedAt': null}", itemId, testId).firstResult();
    }

    public TestItem getInTest(ObjectId testId, ObjectId itemId) {
        TestItem item = findInTest(testId, itemId);
        if (item == null) {
            throw new ValidationException("ITEM_NOT_FOUND", "Soal tidak ditemukan di tes ini");
        }
        return item;
    }

    // =====================================================================
    // COUNT (dipakai TestCatalogService)
    // =====================================================================

    public long countByTest(ObjectId testId) {
        return count("testId = ?1 and deletedAt is null", testId);
    }

    public long countBySection(ObjectId testId, ObjectId sectionId) {
        return count("testId = ?1 and sectionId = ?2 and deletedAt is null", testId, sectionId);
    }

    /** Soal ACTIVE bukan contoh, diurutkan number, untuk ditampilkan ke peserta. */
    public List<TestItem> listActiveBySection(ObjectId testId, ObjectId sectionId) {
        return find("{'testId': ?1, 'sectionId': ?2, 'status': 'ACTIVE', 'isExample': false, 'deletedAt': null}",
                io.quarkus.panache.common.Sort.ascending("number"), testId, sectionId).list();
    }

    /**
     * Menghitung soal yang punya scaleId itu ATAU option scoringnya memetakan ke
     * scaleId.
     * Untuk keperluan cek IN_USE saat hapus scale.
     */
    public long countByScale(ObjectId testId, ObjectId scaleId) {
        // Cek scaleId langsung (LIKERT)
        long byScaleId = count("testId = ?1 and scaleId = ?2 and deletedAt is null", testId, scaleId);
        if (byScaleId > 0)
            return byScaleId;

        // Untuk scaleMappings di options: lakukan di memory karena struktur nested
        // fleksibel
        List<TestItem> allItems = find("testId = ?1 and deletedAt is null", testId).list();
        long byMapping = allItems.stream()
                .filter(item -> itemReferencesScale(item, scaleId))
                .count();
        return byMapping;
    }

    // =====================================================================
    // PRIVATE: VALIDASI
    // =====================================================================

    private void validateScaleMappings(ObjectId testId, List<Map<String, Object>> options) {
        if (options == null)
            return;
        for (Map<String, Object> opt : options) {
            Object scoring = opt.get("scoring");
            if (!(scoring instanceof Map<?, ?> scoringMap))
                continue;
            extractAndValidateScaleIds(testId, scoringMap);
        }
    }

    private void extractAndValidateScaleIds(ObjectId testId, Map<?, ?> scoringMap) {
        // scaleMappings langsung
        Object mappings = scoringMap.get("scaleMappings");
        if (mappings instanceof List<?> list) {
            for (Object m : list) {
                if (m instanceof Map<?, ?> mapping) {
                    Object sid = mapping.get("scaleId");
                    if (sid instanceof String idStr && !idStr.isBlank()) {
                        ObjectId refId = new ObjectId(idStr);
                        if (scales.findInTest(testId, refId) == null) {
                            throw new ValidationException("REFERENCE_NOT_FOUND",
                                    "scaleId '" + idStr + "' di scoring tidak ditemukan di tes ini");
                        }
                    }
                }
            }
        }
        // most/least (MOST_LEAST)
        Object most = scoringMap.get("most");
        if (most instanceof Map<?, ?> mostMap)
            extractAndValidateScaleIds(testId, mostMap);
        Object least = scoringMap.get("least");
        if (least instanceof Map<?, ?> leastMap)
            extractAndValidateScaleIds(testId, leastMap);
    }

    private void validateItemRules(
            ItemType type,
            List<Map<String, Object>> options,
            Boolean reverseScored,
            ObjectId scaleId,
            Double weight,
            Double difficulty,
            Integer timeLimitSeconds) {

        // reverseScored hanya untuk LIKERT, TRUE_FALSE, YES_NO
        if (Boolean.TRUE.equals(reverseScored)
                && type != ItemType.LIKERT
                && type != ItemType.TRUE_FALSE
                && type != ItemType.YES_NO) {
            throw new ValidationException("VALIDATION_FAILED",
                    "reverseScored hanya berlaku untuk LIKERT, TRUE_FALSE, dan YES_NO");
        }

        // weight harus > 0
        if (weight != null && weight <= 0) {
            throw new ValidationException("VALIDATION_FAILED", "weight harus lebih dari 0");
        }

        // difficulty 0-1
        if (difficulty != null && (difficulty < 0 || difficulty > 1)) {
            throw new ValidationException("VALIDATION_FAILED", "difficulty harus antara 0 dan 1");
        }

        // timeLimitSeconds > 0
        if (timeLimitSeconds != null && timeLimitSeconds <= 0) {
            throw new ValidationException("VALIDATION_FAILED", "timeLimitSeconds harus lebih dari 0");
        }

        // LIKERT wajib scaleId
        if (type == ItemType.LIKERT && scaleId == null) {
            throw new ValidationException("VALIDATION_FAILED", "Soal LIKERT wajib memiliki scaleId");
        }

        // Validasi key opsi unik
        if (options != null) {
            long uniqueKeys = options.stream()
                    .map(o -> o.get("key"))
                    .filter(k -> k instanceof String)
                    .distinct()
                    .count();
            if (uniqueKeys < options.size()) {
                throw new ValidationException("VALIDATION_FAILED", "key pada options harus unik dalam satu soal");
            }
        }
    }

    // =====================================================================
    // PRIVATE: FILE LIFECYCLE
    // =====================================================================

    /** Kumpulkan semua file URL dari content dan options. */
    @SuppressWarnings("unchecked")
    private Set<String> extractAllFileUrls(Map<String, Object> content, List<Map<String, Object>> options) {
        Set<String> urls = new HashSet<>();
        if (content != null) {
            Object audioUrl = content.get("audioUrl");
            if (audioUrl instanceof String s && !s.isBlank()) urls.add(s);

            Object images = content.get("images");
            if (images instanceof List<?> imgList) {
                for (Object img : imgList) {
                    if (img instanceof Map<?, ?> imgMap) {
                        Object url = imgMap.get("url");
                        if (url instanceof String s && !s.isBlank()) urls.add(s);
                    }
                }
            }
        }
        if (options != null) {
            for (Map<String, Object> opt : options) {
                Object imageUrl = opt.get("imageUrl");
                if (imageUrl instanceof String s && !s.isBlank()) urls.add(s);
            }
        }
        return urls;
    }

    /**
     * Commit semua URL temp/ di content dan options ke path permanen.
     * Mutasi in-place: replace URL temp dengan URL permanen.
     * Return list URL permanen yang baru di-commit (untuk rollback).
     */
    @SuppressWarnings("unchecked")
    private List<String> commitTempFiles(Map<String, Object> content, List<Map<String, Object>> options) {
        List<String> committed = new ArrayList<>();

        if (content != null) {
            Object audioUrl = content.get("audioUrl");
            if (audioUrl instanceof String s && s.contains("temp/")) {
                String perm = storageService.commitPublicFile(s);
                content.put("audioUrl", perm);
                committed.add(perm);
            }

            Object images = content.get("images");
            if (images instanceof List<?> imgList) {
                for (Object img : imgList) {
                    if (img instanceof Map<?, ?> imgMap) {
                        Object url = imgMap.get("url");
                        if (url instanceof String s && s.contains("temp/")) {
                            String perm = storageService.commitPublicFile(s);
                            ((Map<String, Object>) imgMap).put("url", perm);
                            committed.add(perm);
                        }
                    }
                }
            }
        }

        if (options != null) {
            for (Map<String, Object> opt : options) {
                Object imageUrl = opt.get("imageUrl");
                if (imageUrl instanceof String s && s.contains("temp/")) {
                    String perm = storageService.commitPublicFile(s);
                    opt.put("imageUrl", perm);
                    committed.add(perm);
                }
            }
        }

        return committed;
    }

    /** Rollback: hapus file yang sudah di-commit jika DB gagal. */
    private void rollbackCommittedFiles(List<String> committedUrls) {
        for (String url : committedUrls) {
            try {
                storageService.deletePublicFile(url);
            } catch (Exception ignored) {
                // best-effort cleanup
            }
        }
    }

    // =====================================================================
    // PRIVATE: PEMBANTU
    // =====================================================================

    private void apply(
            TestItem item,
            Integer number,
            Map<String, Object> content,
            List<Map<String, Object>> options,
            String groupId,
            ObjectId scaleId,
            Boolean reverseScored,
            Double weight,
            Boolean scoredInTotal,
            String consistencyGroup,
            Map<String, Object> answerKey,
            Integer timeLimitSeconds,
            Map<String, Object> constraints,
            Double difficulty,
            Boolean isExample,
            ItemStatus status,
            List<String> tags) {

        item.setNumber(number);
        item.setContent(content);
        item.setOptions(options);
        item.setGroupId(groupId);
        item.setScaleId(scaleId);
        item.setReverseScored(reverseScored == null ? Boolean.FALSE : reverseScored);
        item.setWeight(weight == null ? 1.0 : weight);
        item.setScoredInTotal(scoredInTotal == null ? Boolean.TRUE : scoredInTotal);
        item.setConsistencyGroup(consistencyGroup);
        item.setAnswerKey(answerKey);
        item.setTimeLimitSeconds(timeLimitSeconds);
        item.setConstraints(constraints == null ? Map.of() : constraints);
        item.setDifficulty(difficulty);
        item.setIsExample(isExample == null ? Boolean.FALSE : isExample);
        item.setStatus(status == null ? ItemStatus.ACTIVE : status);
        item.setTags(tags == null ? List.of() : tags);
    }

    private boolean numberExists(ObjectId testId, ObjectId sectionId, Integer number, ObjectId excludeItemId) {
        if (excludeItemId == null) {
            return count("testId = ?1 and sectionId = ?2 and number = ?3 and deletedAt is null",
                    testId, sectionId, number) > 0;
        }
        // Saat update: cek di section soal ini, kecuali soal itu sendiri
        List<TestItem> found = find(
                "testId = ?1 and sectionId = ?2 and number = ?3 and deletedAt is null",
                testId, sectionId, number).list();
        return found.stream().anyMatch(i -> !i.getId().equals(excludeItemId));
    }

    private boolean itemReferencesScale(TestItem item, ObjectId scaleId) {
        if (item.getOptions() == null)
            return false;
        for (Map<String, Object> opt : item.getOptions()) {
            Object scoring = opt.get("scoring");
            if (scoring instanceof Map<?, ?> scoringMap && containsScaleId(scoringMap, scaleId)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsScaleId(Map<?, ?> scoringMap, ObjectId targetId) {
        Object mappings = scoringMap.get("scaleMappings");
        if (mappings instanceof List<?> list) {
            for (Object m : list) {
                if (m instanceof Map<?, ?> mapping) {
                    Object sid = mapping.get("scaleId");
                    if (sid instanceof String s && !s.isBlank() && new ObjectId(s).equals(targetId)) {
                        return true;
                    }
                }
            }
        }
        // most/least
        Object most = scoringMap.get("most");
        if (most instanceof Map<?, ?> m && containsScaleId(m, targetId))
            return true;
        Object least = scoringMap.get("least");
        if (least instanceof Map<?, ?> l && containsScaleId(l, targetId))
            return true;
        return false;
    }

    private void syncCounts(ObjectId testId, ObjectId sectionId) {
        // 1. Update itemCount di section
        TestSection section = sections.findInTest(testId, sectionId);
        if (section != null) {
            long sectionCount = countBySection(testId, sectionId);
            section.setItemCount((int) sectionCount);
            section.setUpdatedAt(Instant.now());
            sections.update(section);
        }

        // 2. Update totalItems di test definition (sum semua section)
        TestDefinition test = tests.findById(testId);
        if (test != null) {
            long total = countByTest(testId);
            test.setTotalItems((int) total);
            test.setUpdatedAt(Instant.now());
            tests.update(test);
        }
    }
}
