package com.psycorp.psychapi.feature.test.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

import com.psycorp.psychapi.feature.test.models.TestDefinition;
import com.psycorp.psychapi.feature.test.models.TestScale;
import com.psycorp.psychapi.feature.test.models.TestSection;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TestCatalogService {

    @Inject
    TestDefinitionService tests;

    @Inject
    TestSectionService sections;

    @Inject
    TestScaleService scales;

    @Inject
    TestItemService items;

    @Inject
    NormTableService norms;

    public Map<String, Object> getDetail(ObjectId testId) {
        TestDefinition test = tests.findById(testId);
        if (test == null || test.getDeletedAt() != null) {
            throw new ValidationException("TEST_NOT_FOUND", "Tes tidak ditemukan");
        }

        long sectionCount = sections.countByTest(testId);
        long scaleCount = scales.countByTest(testId);
        long itemCount = items.countByTest(testId);
        long normCount = norms.countByTest(testId);

        Map<String, Object> stats = Map.of(
            "sections", sectionCount,
            "scales", scaleCount,
            "items", itemCount,
            "norms", normCount
        );

        List<TestSection> testSections = sections.listByTest(testId);
        List<Map<String, Object>> progress = new ArrayList<>();
        for (TestSection sec : testSections) {
            long enteredItems = items.countBySection(testId, sec.getId());
            progress.add(Map.of(
                "sectionId", sec.getId().toHexString(),
                "code", sec.getCode(),
                "expectedItems", sec.getItemCount(),
                "enteredItems", enteredItems
            ));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("test", test);
        result.put("stats", stats);
        result.put("progress", progress);
        return result;
    }

    public void deleteTest(ObjectId testId) {
        TestDefinition test = tests.requireDraft(testId);

        items.softDeleteAllByTest(testId);
        norms.softDeleteAllByTest(testId);
        scales.softDeleteAllByTest(testId);
        sections.softDeleteAllByTest(testId);

        test.setDeletedAt(Instant.now());
        tests.update(test);
    }

    public void deleteSection(ObjectId testId, ObjectId sectionId) {
        tests.requireDraft(testId);

        long scaleCount = scales.countBySection(testId, sectionId);
        if (scaleCount > 0) {
            throw new ValidationException("IN_USE", "Section masih dirujuk oleh " + scaleCount + " skala");
        }

        long itemCount = items.countBySection(testId, sectionId);
        if (itemCount > 0) {
            throw new ValidationException("IN_USE", "Section masih memiliki " + itemCount + " soal");
        }

        sections.delete(testId, sectionId);
    }

    public void deleteScale(ObjectId testId, ObjectId scaleId) {
        tests.requireDraft(testId);

        long childCount = scales.countByParent(testId, scaleId);
        if (childCount > 0) {
            throw new ValidationException("IN_USE", "Skala masih memiliki " + childCount + " skala anak");
        }

        TestScale scale = scales.getInTest(testId, scaleId);
        List<String> composites = scales.findCompositeUsing(testId, scale.getCode());
        if (!composites.isEmpty()) {
            throw new ValidationException("IN_USE",
                    "Skala masih dipakai sebagai sumber oleh skala gabungan: " + String.join(", ", composites));
        }

        long itemCount = items.countByScale(testId, scaleId);
        if (itemCount > 0) {
            throw new ValidationException("IN_USE", "Skala masih dirujuk oleh " + itemCount + " soal");
        }

        long normCount = norms.countByScale(testId, scaleId);
        if (normCount > 0) {
            throw new ValidationException("IN_USE", "Skala masih dirujuk oleh " + normCount + " norma");
        }

        scales.delete(testId, scaleId);
    }
}