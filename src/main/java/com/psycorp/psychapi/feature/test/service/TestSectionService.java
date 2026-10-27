package com.psycorp.psychapi.feature.test.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.bson.types.ObjectId;

import com.psycorp.psychapi.feature.test.models.TestDefinition;
import com.psycorp.psychapi.feature.test.models.TestSection;
import com.psycorp.psychapi.feature.test.models.TestSection.DefaultItemType;
import com.psycorp.psychapi.feature.test.models.TestSection.NavigationPolicy;
import com.psycorp.psychapi.feature.test.models.TestSection.SectionStatus;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TestSectionService implements PanacheMongoRepository<TestSection> {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9_]{1,20}$");

    @Inject
    TestDefinitionService tests;

    public TestSection create(
            ObjectId testId,
            String code,
            Integer order,
            String name,
            String instructions,
            List<Map<String, Object>> examples,
            Boolean showExamplesFirst,
            Integer timeLimitSeconds,
            Integer itemCount,
            NavigationPolicy navigationPolicy,
            DefaultItemType defaultItemType,
            Map<String, Object> stimulusPhase,
            Map<String, Object> config,
            SectionStatus status) {

        TestDefinition test = tests.requireDraft(testId);

        if (code == null || !CODE_PATTERN.matcher(code).matches()) {
            throw new ValidationException("VALIDATION_FAILED",
                    "code wajib diisi: huruf besar, angka, dan underscore (1-20 karakter)");
        }
        validateFields(order, name, timeLimitSeconds, itemCount, stimulusPhase);

        if (codeExists(testId, code)) {
            throw new ValidationException("DUPLICATE_CODE",
                    "Section dengan kode " + code + " sudah ada di tes ini");
        }
        if (orderExists(testId, order, null)) {
            throw new ValidationException("ORDER_CONFLICT",
                    "Urutan " + order + " sudah dipakai section lain di tes ini");
        }

        Instant now = Instant.now();

        TestSection section = new TestSection();
        section.setTestId(test.getId());
        section.setTestVersion(test.getVersion());
        section.setCode(code);
        apply(section, order, name, instructions, examples, showExamplesFirst, timeLimitSeconds,
                itemCount, navigationPolicy, defaultItemType, stimulusPhase, config, status);
        section.setScaleIds(new ArrayList<>());
        section.setCreatedAt(now);
        section.setUpdatedAt(now);

        persist(section);
        return section;
    }

    public TestSection update(
            ObjectId testId,
            ObjectId sectionId,
            Integer order,
            String name,
            String instructions,
            List<Map<String, Object>> examples,
            Boolean showExamplesFirst,
            Integer timeLimitSeconds,
            Integer itemCount,
            NavigationPolicy navigationPolicy,
            DefaultItemType defaultItemType,
            Map<String, Object> stimulusPhase,
            Map<String, Object> config,
            SectionStatus status) {

        tests.requireDraft(testId);
        TestSection section = getInTest(testId, sectionId);

        validateFields(order, name, timeLimitSeconds, itemCount, stimulusPhase);

        if (orderExists(testId, order, sectionId)) {
            throw new ValidationException("ORDER_CONFLICT",
                    "Urutan " + order + " sudah dipakai section lain di tes ini");
        }

        apply(section, order, name, instructions, examples, showExamplesFirst, timeLimitSeconds,
                itemCount, navigationPolicy, defaultItemType, stimulusPhase, config, status);
        section.setUpdatedAt(Instant.now());

        update(section);
        return section;
    }

    public List<TestSection> listByTest(ObjectId testId) {
        return find("testId = ?1 and deletedAt is null", Sort.ascending("order"), testId).list();
    }

    public void delete(ObjectId testId, ObjectId sectionId) {
        tests.requireDraft(testId);
        TestSection section = getInTest(testId, sectionId);

        section.setDeletedAt(Instant.now());
        update(section);
    }

    public long countByTest(ObjectId testId) {
        return count("testId = ?1 and deletedAt is null", testId);
    }

    public void softDeleteAllByTest(ObjectId testId) {
        update("deletedAt = ?1 where testId = ?2 and deletedAt is null", Instant.now(), testId);
    }
    // =====================================================================
    // LOOKUP (dipakai TestScaleService dan, nanti, TestItemService)
    // =====================================================================

    /** Mengembalikan section di dalam tes tertentu, atau null jika tidak ada. */
    public TestSection findInTest(ObjectId testId, ObjectId sectionId) {
        return find("{'_id': ?1, 'testId': ?2, 'deletedAt': null}", sectionId, testId).firstResult();
    }

    /** Sama seperti findInTest, tetapi melempar error jika tidak ada. */
    public TestSection getInTest(ObjectId testId, ObjectId sectionId) {
        TestSection section = findInTest(testId, sectionId);
        if (section == null) {
            throw new ValidationException("SECTION_NOT_FOUND", "Section tidak ditemukan di tes ini");
        }
        return section;
    }

    /** Mencari section lewat kode (unik per tes). Null jika tidak ada. */
    public TestSection findByCode(ObjectId testId, String code) {
        return find("{'testId': ?1, 'code': ?2, 'deletedAt': null}", testId, code).firstResult();
    }

    public void attachScale(ObjectId sectionId, ObjectId scaleId) {
        TestSection section = findById(sectionId);
        if (section == null) {
            return;
        }
        List<ObjectId> ids = section.getScaleIds() == null
                ? new ArrayList<>()
                : new ArrayList<>(section.getScaleIds());
        if (!ids.contains(scaleId)) {
            ids.add(scaleId);
            section.setScaleIds(ids);
            section.setUpdatedAt(Instant.now());
            update(section);
        }
    }

    public void detachScale(ObjectId sectionId, ObjectId scaleId) {
        TestSection section = findById(sectionId);
        if (section == null || section.getScaleIds() == null) {
            return;
        }
        List<ObjectId> ids = new ArrayList<>(section.getScaleIds());
        if (ids.remove(scaleId)) {
            section.setScaleIds(ids);
            section.setUpdatedAt(Instant.now());
            update(section);
        }
    }

    private boolean codeExists(ObjectId testId, String code) {
        return count("{'testId': ?1, 'code': ?2, 'deletedAt': null}", testId, code) > 0;
    }

    /**
     * excludeSectionId diisi saat update agar section itu sendiri tidak dihitung
     * bentrok.
     */
    private boolean orderExists(ObjectId testId, Integer order, ObjectId excludeSectionId) {
        if (excludeSectionId == null) {
            return count("{'testId': ?1, 'order': ?2, 'deletedAt': null}", testId, order) > 0;
        }
        return count("{'testId': ?1, 'order': ?2, 'deletedAt': null, '_id': {'$ne': ?3}}",
                testId, order, excludeSectionId) > 0;
    }

    private void validateFields(Integer order, String name, Integer timeLimitSeconds,
            Integer itemCount, Map<String, Object> stimulusPhase) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("VALIDATION_FAILED", "name wajib diisi");
        }
        if (order == null || order < 1) {
            throw new ValidationException("VALIDATION_FAILED", "order wajib diisi dan minimal 1");
        }
        if (timeLimitSeconds != null && timeLimitSeconds <= 0) {
            throw new ValidationException("VALIDATION_FAILED", "timeLimitSeconds harus lebih dari 0 jika diisi");
        }
        if (itemCount != null && itemCount < 0) {
            throw new ValidationException("VALIDATION_FAILED", "itemCount tidak boleh negatif");
        }
        if (stimulusPhase != null) {
            Object contentType = stimulusPhase.get("contentType");
            Object displaySeconds = stimulusPhase.get("displaySeconds");
            if (!(contentType instanceof String type) || type.isBlank()) {
                throw new ValidationException("VALIDATION_FAILED", "stimulusPhase.contentType wajib diisi");
            }
            if (!(displaySeconds instanceof Number seconds) || seconds.intValue() <= 0) {
                throw new ValidationException("VALIDATION_FAILED",
                        "stimulusPhase.displaySeconds wajib diisi dan lebih dari 0");
            }
        }
    }

    /**
     * Menyalin field yang boleh diubah, lengkap dengan nilai default. Dipakai
     * create dan update.
     */
    private void apply(TestSection section, Integer order, String name, String instructions,
            List<Map<String, Object>> examples, Boolean showExamplesFirst, Integer timeLimitSeconds,
            Integer itemCount, NavigationPolicy navigationPolicy, DefaultItemType defaultItemType,
            Map<String, Object> stimulusPhase, Map<String, Object> config, SectionStatus status) {
        section.setOrder(order);
        section.setName(name.trim());
        section.setInstructions(instructions == null ? "" : instructions);
        section.setExamples(examples == null ? new ArrayList<>() : examples);
        section.setShowExamplesFirst(showExamplesFirst == null ? Boolean.TRUE : showExamplesFirst);
        section.setTimeLimitSeconds(timeLimitSeconds);
        section.setItemCount(itemCount == null ? 0 : itemCount);
        section.setNavigationPolicy(navigationPolicy == null ? NavigationPolicy.FREE : navigationPolicy);
        section.setDefaultItemType(defaultItemType == null ? DefaultItemType.SINGLE_CHOICE : defaultItemType);
        section.setStimulusPhase(stimulusPhase);
        section.setConfig(config == null ? new HashMap<>() : config);
        section.setStatus(status == null ? SectionStatus.ACTIVE : status);
    }
}