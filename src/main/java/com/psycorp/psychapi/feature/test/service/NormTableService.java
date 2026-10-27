package com.psycorp.psychapi.feature.test.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

import com.psycorp.psychapi.feature.test.api.dto.request.NormCreateRequest;
import com.psycorp.psychapi.feature.test.api.dto.request.NormUpdateRequest;
import com.psycorp.psychapi.feature.test.models.NormTable;
import com.psycorp.psychapi.feature.test.models.NormTable.NormMethod;
import com.psycorp.psychapi.feature.test.models.NormTable.NormStatus;
import com.psycorp.psychapi.feature.test.models.TestScale;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class NormTableService implements PanacheMongoRepository<NormTable> {

    @Inject
    TestDefinitionService tests;

    @Inject
    TestScaleService scales;

    // =====================================================================
    // CRUD
    // =====================================================================

    public NormTable create(ObjectId testId, NormCreateRequest req) {
        var test = tests.requireNotArchived(testId);

        TestScale scale = scales.getInTest(testId, req.scaleId());
        validateScoreType(scale);

        validateBody(req.method(), req.entries(), req.mean(), req.sd());

        Instant now = Instant.now();
        NormTable norm = new NormTable();
        norm.setTestId(testId);
        norm.setTestVersion(test.getVersion());
        norm.setScaleId(req.scaleId());
        norm.setSectionId(scale.getSectionId());
        apply(norm, req.normVersion(), req.name(), req.targetScoreType(), req.method(),
                req.criteria(), req.entries(), req.mean(), req.sd(),
                req.categoryCutoffs(), req.sampleSize(), req.source(),
                req.validFrom(), req.validUntil());
        norm.setStatus(NormStatus.DRAFT);
        norm.setCreatedAt(now);
        norm.setUpdatedAt(now);

        persist(norm);
        return norm;
    }

    public NormTable update(ObjectId testId, ObjectId normId, NormUpdateRequest req) {
        tests.requireNotArchived(testId);
        NormTable norm = getInTest(testId, normId);

        if (norm.getStatus() != NormStatus.DRAFT) {
            throw new ValidationException("INVALID_STATE", "Hanya norma DRAFT yang bisa diubah");
        }

        validateBody(req.method(), req.entries(), req.mean(), req.sd());

        apply(norm, req.normVersion(), req.name(), req.targetScoreType(), req.method(),
                req.criteria(), req.entries(), req.mean(), req.sd(),
                req.categoryCutoffs(), req.sampleSize(), req.source(),
                req.validFrom(), req.validUntil());
        norm.setUpdatedAt(Instant.now());

        update(norm);
        return norm;
    }

    public NormTable getInTest(ObjectId testId, ObjectId normId) {
        NormTable norm = find("{'_id': ?1, 'testId': ?2, 'deletedAt': null}", normId, testId).firstResult();
        if (norm == null) {
            throw new ValidationException("NORM_NOT_FOUND", "Norma tidak ditemukan di tes ini");
        }
        return norm;
    }

    public List<NormTable> listByTest(ObjectId testId, ObjectId scaleId, String status) {
        if (scaleId != null && status != null) {
            return find("testId = ?1 and scaleId = ?2 and status = ?3 and deletedAt is null",
                    Sort.ascending("createdAt"), testId, scaleId, status).list();
        }
        if (scaleId != null) {
            return find("testId = ?1 and scaleId = ?2 and deletedAt is null",
                    Sort.ascending("createdAt"), testId, scaleId).list();
        }
        if (status != null) {
            return find("testId = ?1 and status = ?2 and deletedAt is null",
                    Sort.ascending("createdAt"), testId, status).list();
        }
        return find("testId = ?1 and deletedAt is null", Sort.ascending("createdAt"), testId).list();
    }

    public void delete(ObjectId testId, ObjectId normId) {
        NormTable norm = getInTest(testId, normId);
        if (norm.getStatus() != NormStatus.DRAFT) {
            throw new ValidationException("INVALID_STATE", "Hanya norma DRAFT yang bisa dihapus");
        }
        norm.setDeletedAt(Instant.now());
        update(norm);
    }

    public NormTable activate(ObjectId testId, ObjectId normId) {
        tests.requireNotArchived(testId);
        NormTable norm = getInTest(testId, normId);

        if (norm.getStatus() != NormStatus.DRAFT) {
            throw new ValidationException("INVALID_STATE", "Hanya norma DRAFT yang bisa diaktifkan");
        }

        // Cek tidak bertumpuk dengan norma ACTIVE lain di skala yang sama
        checkOverlap(testId, norm);

        // Cek coverage jika LOOKUP dan skala punya rawMin/rawMax
        if (norm.getMethod() == NormMethod.LOOKUP) {
            TestScale scale = scales.findInTest(testId, norm.getScaleId());
            if (scale != null && scale.getRawMin() != null && scale.getRawMax() != null) {
                Map<String, Object> coverage = computeCoverage(norm.getEntries(), scale.getRawMin(), scale.getRawMax());
                if (Boolean.FALSE.equals(coverage.get("covered"))) {
                    throw new ValidationException("NORM_INCOMPLETE",
                            "Norma belum mencakup seluruh rentang skor mentah. Gaps: " + coverage.get("gaps"));
                }
            }
        }

        norm.setStatus(NormStatus.ACTIVE);
        norm.setUpdatedAt(Instant.now());
        update(norm);
        return norm;
    }

    public NormTable archive(ObjectId testId, ObjectId normId) {
        NormTable norm = getInTest(testId, normId);
        if (norm.getStatus() != NormStatus.ACTIVE) {
            throw new ValidationException("INVALID_STATE", "Hanya norma ACTIVE yang bisa diarsipkan");
        }
        norm.setStatus(NormStatus.ARCHIVED);
        norm.setUpdatedAt(Instant.now());
        update(norm);
        return norm;
    }

    // =====================================================================
    // DIPAKAI SERVICE LAIN
    // =====================================================================

    public long countByTest(ObjectId testId) {
        return count("testId = ?1 and deletedAt is null", testId);
    }

    public long countByScale(ObjectId testId, ObjectId scaleId) {
        return count("testId = ?1 and scaleId = ?2 and deletedAt is null", testId, scaleId);
    }

    public void softDeleteAllByTest(ObjectId testId) {
        update("deletedAt = ?1 where testId = ?2 and deletedAt is null", Instant.now(), testId);
    }

    // =====================================================================
    // PRIVATE: HELPER
    // =====================================================================

    private void apply(NormTable norm, String normVersion, String name, String targetScoreType,
            NormMethod method, Map<String, Object> criteria,
            List<Map<String, Object>> entries, Double mean, Double sd,
            List<Map<String, Object>> categoryCutoffs, Integer sampleSize, String source,
            Instant validFrom, Instant validUntil) {
        if (normVersion == null || normVersion.isBlank()) {
            throw new ValidationException("VALIDATION_FAILED", "normVersion wajib diisi");
        }
        if (name == null || name.isBlank()) {
            throw new ValidationException("VALIDATION_FAILED", "name wajib diisi");
        }
        if (targetScoreType == null || targetScoreType.isBlank()) {
            throw new ValidationException("VALIDATION_FAILED", "targetScoreType wajib diisi");
        }
        norm.setNormVersion(normVersion);
        norm.setName(name);
        norm.setTargetScoreType(targetScoreType);
        norm.setMethod(method != null ? method : NormMethod.LOOKUP);
        norm.setCriteria(criteria);
        norm.setEntries(entries != null ? entries : List.of());
        norm.setMean(mean);
        norm.setSd(sd);
        norm.setCategoryCutoffs(categoryCutoffs != null ? categoryCutoffs : List.of());
        norm.setSampleSize(sampleSize);
        norm.setSource(source);
        norm.setValidFrom(validFrom != null ? validFrom : Instant.now());
        norm.setValidUntil(validUntil);
    }

    private void validateBody(NormMethod method, List<Map<String, Object>> entries,
            Double mean, Double sd) {
        NormMethod resolved = method != null ? method : NormMethod.LOOKUP;
        if (resolved == NormMethod.LOOKUP) {
            if (entries == null || entries.isEmpty()) {
                throw new ValidationException("VALIDATION_FAILED", "entries wajib diisi untuk metode LOOKUP");
            }
            validateEntries(entries);
        } else {
            // MEAN_SD
            if (mean == null) {
                throw new ValidationException("VALIDATION_FAILED", "mean wajib diisi untuk metode MEAN_SD");
            }
            if (sd == null || sd <= 0) {
                throw new ValidationException("VALIDATION_FAILED", "sd wajib diisi dan lebih dari 0");
            }
            if (entries != null && !entries.isEmpty()) {
                throw new ValidationException("VALIDATION_FAILED", "entries harus kosong untuk metode MEAN_SD");
            }
        }
    }

    private void validateEntries(List<Map<String, Object>> entries) {
        // Kumpulkan rentang untuk cek overlap
        List<int[]> ranges = new ArrayList<>();
        for (Map<String, Object> entry : entries) {
            Integer rawMin = toInt(entry.get("rawMin"));
            Integer rawMax = toInt(entry.get("rawMax"));
            if (rawMin == null || rawMax == null) {
                throw new ValidationException("VALIDATION_FAILED", "Setiap baris entries wajib punya rawMin dan rawMax");
            }
            if (rawMin > rawMax) {
                throw new ValidationException("VALIDATION_FAILED",
                        "rawMin tidak boleh lebih besar dari rawMax pada baris entries");
            }
            // Tiap baris wajib punya standard, atau pasangan standardMin+standardMax
            boolean hasStandard = entry.get("standard") != null;
            boolean hasRange = entry.get("standardMin") != null && entry.get("standardMax") != null;
            if (!hasStandard && !hasRange) {
                throw new ValidationException("VALIDATION_FAILED",
                        "Setiap baris entries wajib punya 'standard' atau pasangan 'standardMin' dan 'standardMax'");
            }
            ranges.add(new int[]{rawMin, rawMax});
        }
        // Cek overlap antar baris
        for (int i = 0; i < ranges.size(); i++) {
            for (int j = i + 1; j < ranges.size(); j++) {
                if (ranges.get(i)[0] <= ranges.get(j)[1] && ranges.get(j)[0] <= ranges.get(i)[1]) {
                    throw new ValidationException("VALIDATION_FAILED",
                            "Baris entries tidak boleh bertumpuk");
                }
            }
        }
    }

    private void validateScoreType(TestScale scale) {
        String st = scale.getScoreType() != null ? scale.getScoreType().getValue() : "";
        if (!st.startsWith("RAW_TO_")) {
            throw new ValidationException("VALIDATION_FAILED",
                    "Norma hanya bisa dibuat untuk skala dengan scoreType RAW_TO_*");
        }
    }

    private void checkOverlap(ObjectId testId, NormTable candidate) {
        List<NormTable> activeNorms = find(
                "testId = ?1 and scaleId = ?2 and status = 'ACTIVE' and deletedAt is null",
                testId, candidate.getScaleId()).list();

        Map<String, Object> cc = candidate.getCriteria();
        for (NormTable existing : activeNorms) {
            Map<String, Object> ec = existing.getCriteria();
            if (criteriaOverlap(cc, ec)) {
                throw new ValidationException("NORM_OVERLAP",
                        "Sudah ada norma ACTIVE untuk skala dan kelompok yang beririsan");
            }
        }
    }

    /** Dua criteria beririsan jika gender, education, dan rentang usia bersinggungan. */
    private boolean criteriaOverlap(Map<String, Object> a, Map<String, Object> b) {
        if (a == null || b == null) return true;

        // Gender: beririsan kecuali keduanya berbeda dan tidak ada ANY
        String gA = strVal(a, "gender", "ANY");
        String gB = strVal(b, "gender", "ANY");
        if (!gA.equals("ANY") && !gB.equals("ANY") && !gA.equals(gB)) return false;

        // Education: sama seperti gender
        String eA = strVal(a, "education", "ANY");
        String eB = strVal(b, "education", "ANY");
        if (!eA.equals("ANY") && !eB.equals("ANY") && !eA.equals(eB)) return false;

        // Usia: rentang [ageMin, ageMax] beririsan (null = tanpa batas)
        Integer aMin = toInt(a.get("ageMin"));
        Integer aMax = toInt(a.get("ageMax"));
        Integer bMin = toInt(b.get("ageMin"));
        Integer bMax = toInt(b.get("ageMax"));

        int lo1 = aMin != null ? aMin : 0;
        int hi1 = aMax != null ? aMax : Integer.MAX_VALUE;
        int lo2 = bMin != null ? bMin : 0;
        int hi2 = bMax != null ? bMax : Integer.MAX_VALUE;

        return lo1 <= hi2 && lo2 <= hi1;
    }

    /** Menghitung celah coverage entries dibanding rawMin-rawMax skala. */
    public Map<String, Object> computeCoverage(List<Map<String, Object>> entries,
            Integer rawMin, Integer rawMax) {
        if (rawMin == null || rawMax == null || entries == null) {
            return Map.of("rawMin", rawMin, "rawMax", rawMax, "covered", false, "gaps", List.of());
        }

        boolean[] covered = new boolean[rawMax - rawMin + 1];
        for (Map<String, Object> entry : entries) {
            Integer eMin = toInt(entry.get("rawMin"));
            Integer eMax = toInt(entry.get("rawMax"));
            if (eMin == null || eMax == null) continue;
            int lo = Math.max(eMin, rawMin);
            int hi = Math.min(eMax, rawMax);
            for (int i = lo; i <= hi; i++) {
                covered[i - rawMin] = true;
            }
        }

        List<Map<String, Object>> gaps = new ArrayList<>();
        int gapStart = -1;
        for (int i = 0; i <= covered.length; i++) {
            boolean uncovered = i < covered.length && !covered[i];
            if (uncovered && gapStart == -1) {
                gapStart = i + rawMin;
            } else if (!uncovered && gapStart != -1) {
                gaps.add(Map.of("from", gapStart, "to", i + rawMin - 1));
                gapStart = -1;
            }
        }

        return Map.of(
                "rawMin", rawMin,
                "rawMax", rawMax,
                "covered", gaps.isEmpty(),
                "gaps", gaps);
    }

    private String strVal(Map<String, Object> map, String key, String defaultVal) {
        Object v = map.get(key);
        return v != null ? String.valueOf(v) : defaultVal;
    }

    private Integer toInt(Object v) {
        if (v instanceof Integer i) return i;
        if (v instanceof Number n) return n.intValue();
        return null;
    }
}
