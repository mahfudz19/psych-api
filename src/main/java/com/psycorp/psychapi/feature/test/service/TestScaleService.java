package com.psycorp.psychapi.feature.test.service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.bson.types.ObjectId;

import com.psycorp.psychapi.feature.test.models.TestScale;
import com.psycorp.psychapi.feature.test.models.TestScale.ScaleRole;
import com.psycorp.psychapi.feature.test.models.TestScale.ScaleType;
import com.psycorp.psychapi.feature.test.models.TestScale.ScoreType;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TestScaleService implements PanacheMongoRepository<TestScale> {

    // Huruf kecil diizinkan karena ada skala seperti MMPI "Hs", "Hy", "Pd".
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z0-9_]{1,30}$");
    private static final Pattern COLOR_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final Set<String> COMPOSITE_METHODS = Set.of("SUM", "MEAN");

    @Inject
    TestDefinitionService tests;

    @Inject
    TestSectionService sections;

    public TestScale create(
            ObjectId testId,
            String code,
            String name,
            ScaleType type,
            ScaleRole role,
            ObjectId parentScaleId,
            ObjectId sectionId,
            Integer order,
            String description,
            String lowLabel,
            String highLabel,
            Integer rawMin,
            Integer rawMax,
            ScoreType scoreType,
            Map<String, Object> composite,
            Boolean isReported,
            Boolean isValidityScale,
            String color) {

        tests.requireDraft(testId);

        if (code == null || !CODE_PATTERN.matcher(code).matches()) {
            throw new ValidationException("VALIDATION_FAILED", "code wajib diisi: huruf, angka, dan underscore (1-30 karakter)");
        }
        if (codeExists(testId, code)) {
            throw new ValidationException("DUPLICATE_CODE", "Skala dengan kode " + code + " sudah ada di tes ini");
        }

        ScaleType resolvedType = type == null ? ScaleType.DIMENSION : type;
        validateFields(name, resolvedType, sectionId, rawMin, rawMax, color);
        validateParent(testId, parentScaleId, null);
        validateSection(testId, sectionId);
        Map<String, Object> normalizedComposite = validateComposite(testId, code, resolvedType, composite);

        TestScale scale = new TestScale();
        scale.setTestId(testId);
        scale.setCode(code);
        apply(scale, name, resolvedType, role, parentScaleId, sectionId,
                order == null ? nextOrder(testId) : order,
                description, lowLabel, highLabel, rawMin, rawMax, scoreType,
                normalizedComposite, isReported, isValidityScale, color);

        Instant now = Instant.now();
        scale.setCreatedAt(now);
        scale.setUpdatedAt(now);

        persist(scale);

        if (sectionId != null) {
            sections.attachScale(sectionId, scale.id);
        }
        return scale;
    }

    public TestScale update(
            ObjectId testId,
            ObjectId scaleId,
            String name,
            ScaleType type,
            ScaleRole role,
            ObjectId parentScaleId,
            ObjectId sectionId,
            Integer order,
            String description,
            String lowLabel,
            String highLabel,
            Integer rawMin,
            Integer rawMax,
            ScoreType scoreType,
            Map<String, Object> composite,
            Boolean isReported,
            Boolean isValidityScale,
            String color) {

        tests.requireDraft(testId);
        TestScale scale = getInTest(testId, scaleId);

        ScaleType resolvedType = type == null ? ScaleType.DIMENSION : type;
        validateFields(name, resolvedType, sectionId, rawMin, rawMax, color);
        validateParent(testId, parentScaleId, scaleId);
        validateSection(testId, sectionId);
        Map<String, Object> normalizedComposite =
                validateComposite(testId, scale.getCode(), resolvedType, composite);

        ObjectId oldSectionId = scale.getSectionId();

        apply(scale, name, resolvedType, role, parentScaleId, sectionId,
                order == null ? scale.getOrder() : order,
                description, lowLabel, highLabel, rawMin, rawMax, scoreType,
                normalizedComposite, isReported, isValidityScale, color);
        scale.setUpdatedAt(Instant.now());

        update(scale);

        if (!Objects.equals(oldSectionId, sectionId)) {
            if (oldSectionId != null) {
                sections.detachScale(oldSectionId, scale.id);
            }
            if (sectionId != null) {
                sections.attachScale(sectionId, scale.id);
            }
        }
        return scale;
    }

    public List<TestScale> listByTest(ObjectId testId, ObjectId sectionId) {
        if (sectionId != null) {
            return find("testId = ?1 and sectionId = ?2 and deletedAt is null", Sort.ascending("order"), testId, sectionId).list();
        }
        return find("testId = ?1 and deletedAt is null", Sort.ascending("order"), testId).list();
    }

    public void delete(ObjectId testId, ObjectId scaleId) {
        tests.requireDraft(testId);
        TestScale scale = getInTest(testId, scaleId);
        
        scale.setDeletedAt(Instant.now());
        update(scale);
        
        // Lepaskan dari scaleIds milik section jika ada
        if (scale.getSectionId() != null) {
            sections.detachScale(scale.getSectionId(), scale.id);
        }
    }

    public long countByTest(ObjectId testId) {
        return count("testId = ?1 and deletedAt is null", testId);
    }

    public long countBySection(ObjectId testId, ObjectId sectionId) {
        return count("testId = ?1 and sectionId = ?2 and deletedAt is null", testId, sectionId);
    }

    public long countByParent(ObjectId testId, ObjectId parentScaleId) {
        return count("testId = ?1 and parentScaleId = ?2 and deletedAt is null", testId, parentScaleId);
    }

    public List<String> findCompositeUsing(ObjectId testId, String scaleCode) {
        // Mencari skala composite yang sourceScaleCodes-nya mengandung scaleCode
        List<TestScale> composites = find("testId = ?1 and type = 'COMPOSITE' and deletedAt is null", testId).list();
        List<String> result = new ArrayList<>();
        
        for (TestScale scale : composites) {
            if (scale.getComposite() != null) {
                Object sources = scale.getComposite().get("sourceScaleCodes");
                if (sources instanceof List<?> list && list.contains(scaleCode)) {
                    result.add(scale.getCode());
                }
            }
        }
        return result;
    }

    public void softDeleteAllByTest(ObjectId testId) {
        update("deletedAt = ?1 where testId = ?2 and deletedAt is null", Instant.now(), testId);
    }

    // =====================================================================
    // LOOKUP (dipakai TestItemService, norma, dan template nanti)
    // =====================================================================

    /** Mengembalikan skala di dalam tes tertentu, atau null jika tidak ada. */
    public TestScale findInTest(ObjectId testId, ObjectId scaleId) {
        return find("{'_id': ?1, 'testId': ?2, 'deletedAt': null}", scaleId, testId).firstResult();
    }

    /** Sama seperti findInTest, tetapi melempar error jika tidak ada. */
    public TestScale getInTest(ObjectId testId, ObjectId scaleId) {
        TestScale scale = findInTest(testId, scaleId);
        if (scale == null) {
            throw new ValidationException("SCALE_NOT_FOUND", "Skala tidak ditemukan di tes ini");
        }
        return scale;
    }

    /** Mencari skala lewat kode (unik per tes). Null jika tidak ada. */
    public TestScale findByCode(ObjectId testId, String code) {
        return find("{'testId': ?1, 'code': ?2, 'deletedAt': null}", testId, code).firstResult();
    }

    // =====================================================================
    // PRIVATE: VALIDASI
    // =====================================================================

    private void validateFields(String name, ScaleType type, ObjectId sectionId,
            Integer rawMin, Integer rawMax, String color) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("VALIDATION_FAILED", "name wajib diisi");
        }
        if (type == ScaleType.SUBTEST && sectionId == null) {
            throw new ValidationException("VALIDATION_FAILED",
                    "Skala bertipe SUBTEST wajib punya sectionId");
        }
        if (rawMin != null && rawMax != null && rawMin > rawMax) {
            throw new ValidationException("VALIDATION_FAILED", "rawMin tidak boleh lebih besar dari rawMax");
        }
        if (color != null && !COLOR_PATTERN.matcher(color).matches()) {
            throw new ValidationException("VALIDATION_FAILED", "color harus berformat hex #RRGGBB");
        }
    }

    /**
     * Skala induk harus ada di tes yang sama dan tidak boleh membentuk siklus.
     * selfId bernilai null saat create (skala belum punya id).
     */
    private void validateParent(ObjectId testId, ObjectId parentScaleId, ObjectId selfId) {
        ObjectId cursor = parentScaleId;
        Set<ObjectId> visited = new HashSet<>();
        while (cursor != null) {
            if (cursor.equals(selfId) || !visited.add(cursor)) {
                throw new ValidationException("SCALE_CYCLE", "parentScaleId membentuk siklus");
            }
            TestScale parent = findInTest(testId, cursor);
            if (parent == null) {
                throw new ValidationException("REFERENCE_NOT_FOUND",
                        "Skala induk tidak ditemukan di tes ini");
            }
            cursor = parent.getParentScaleId();
        }
    }

    private void validateSection(ObjectId testId, ObjectId sectionId) {
        if (sectionId != null && sections.findInTest(testId, sectionId) == null) {
            throw new ValidationException("REFERENCE_NOT_FOUND", "Section tidak ditemukan di tes ini");
        }
    }

    /**
     * Memvalidasi skor gabungan dan mengembalikan bentuk yang akan disimpan.
     * Hasilnya null jika type bukan COMPOSITE.
     */
    private Map<String, Object> validateComposite(ObjectId testId, String selfCode,
            ScaleType type, Map<String, Object> composite) {
        if (type != ScaleType.COMPOSITE) {
            if (composite != null) {
                throw new ValidationException("VALIDATION_FAILED",
                        "composite hanya boleh diisi untuk skala bertipe COMPOSITE");
            }
            return null;
        }
        if (composite == null) {
            throw new ValidationException("VALIDATION_FAILED",
                    "Skala bertipe COMPOSITE wajib punya composite");
        }

        Object rawSources = composite.get("sourceScaleCodes");
        if (!(rawSources instanceof List<?> list) || list.isEmpty()) {
            throw new ValidationException("VALIDATION_FAILED",
                    "composite.sourceScaleCodes wajib berisi minimal satu kode skala");
        }
        List<String> sourceCodes = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof String s) || s.isBlank()) {
                throw new ValidationException("VALIDATION_FAILED",
                        "composite.sourceScaleCodes hanya boleh berisi kode skala berupa teks");
            }
            if (!sourceCodes.contains(s)) {
                sourceCodes.add(s);
            }
        }

        Object rawMethod = composite.get("method");
        String method = rawMethod == null ? "SUM" : String.valueOf(rawMethod).toUpperCase();
        if (!COMPOSITE_METHODS.contains(method)) {
            throw new ValidationException("VALIDATION_FAILED", "composite.method harus SUM atau MEAN");
        }

        for (String sourceCode : sourceCodes) {
            if (sourceCode.equals(selfCode)) {
                throw new ValidationException("SCALE_CYCLE", "Skala gabungan tidak boleh merujuk dirinya sendiri");
            }
            if (findByCode(testId, sourceCode) == null) {
                throw new ValidationException("REFERENCE_NOT_FOUND",
                        "Skala sumber '" + sourceCode + "' tidak ditemukan di tes ini");
            }
        }
        assertNoCompositeCycle(testId, selfCode, sourceCodes);

        Map<String, Object> normalized = new HashMap<>();
        normalized.put("sourceScaleCodes", sourceCodes);
        normalized.put("method", method);
        return normalized;
    }

    /** Menelusuri skala gabungan lain yang menjadi sumber, memastikan tidak kembali ke selfCode. */
    private void assertNoCompositeCycle(ObjectId testId, String selfCode, List<String> sourceCodes) {
        Set<String> visited = new HashSet<>();
        Deque<String> stack = new ArrayDeque<>(sourceCodes);
        while (!stack.isEmpty()) {
            String current = stack.pop();
            if (current.equals(selfCode)) {
                throw new ValidationException("SCALE_CYCLE", "Skala gabungan membentuk siklus");
            }
            if (!visited.add(current)) {
                continue;
            }
            TestScale source = findByCode(testId, current);
            if (source != null && source.getType() == ScaleType.COMPOSITE && source.getComposite() != null
                    && source.getComposite().get("sourceScaleCodes") instanceof List<?> nested) {
                for (Object o : nested) {
                    if (o instanceof String s) {
                        stack.push(s);
                    }
                }
            }
        }
    }

    // =====================================================================
    // PRIVATE: PEMBANTU
    // =====================================================================

    private boolean codeExists(ObjectId testId, String code) {
        return count("{'testId': ?1, 'code': ?2, 'deletedAt': null}", testId, code) > 0;
    }

    private int nextOrder(ObjectId testId) {
        TestScale last = find("{'testId': ?1, 'deletedAt': null}", Sort.descending("order"), testId)
                .firstResult();
        return (last == null || last.getOrder() == null) ? 1 : last.getOrder() + 1;
    }

    /** Menyalin field yang boleh diubah, lengkap dengan nilai default. Dipakai create dan update. */
    private void apply(TestScale scale, String name, ScaleType type, ScaleRole role,
            ObjectId parentScaleId, ObjectId sectionId, Integer order, String description,
            String lowLabel, String highLabel, Integer rawMin, Integer rawMax, ScoreType scoreType,
            Map<String, Object> composite, Boolean isReported, Boolean isValidityScale, String color) {
        scale.setName(name.trim());
        scale.setType(type);
        scale.setRole(role == null ? ScaleRole.PERFORMANCE : role);
        scale.setParentScaleId(parentScaleId);
        scale.setSectionId(sectionId);
        scale.setOrder(order);
        scale.setDescription(description == null ? "" : description);
        scale.setLowLabel(lowLabel);
        scale.setHighLabel(highLabel);
        scale.setRawMin(rawMin);
        scale.setRawMax(rawMax);
        scale.setScoreType(scoreType == null ? ScoreType.RAW_ONLY : scoreType);
        scale.setComposite(composite);
        scale.setIsReported(isReported == null ? Boolean.TRUE : isReported);
        scale.setIsValidityScale(isValidityScale == null ? Boolean.FALSE : isValidityScale);
        scale.setColor(color);
    }
}