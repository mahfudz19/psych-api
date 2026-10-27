package com.psycorp.psychapi.feature.test.service;

import java.time.Instant;
import java.util.List;

import org.bson.types.ObjectId;

import com.psycorp.psychapi.feature.test.api.dto.request.TestDefinitionCreateRequest;
import com.psycorp.psychapi.feature.test.api.dto.request.TestDefinitionUpdateRequest;
import com.psycorp.psychapi.feature.test.models.TestDefinition;
import com.psycorp.psychapi.feature.test.models.TestDefinition.TestPurpose;
import com.psycorp.psychapi.feature.test.models.TestDefinition.TestStatus;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TestDefinitionService implements PanacheMongoRepository<TestDefinition> {
    
    public TestDefinition create(TestDefinitionCreateRequest req) {
        String version = req.version() != null ? req.version() : "1.0.0";
        
        // Cek duplikat berdasarkan (code, version)
        if (find("code = ?1 and version = ?2 and deletedAt is null", req.code(), version).firstResult() != null) {
            throw new ValidationException("DUPLICATE_CODE", "Test definition dengan kode " + req.code() + " dan versi " + version + " sudah ada");
        }

        Instant now = Instant.now();

        TestDefinition entity = new TestDefinition();
        entity.setCode(req.code());
        entity.setName(req.name());
        entity.setShortName(req.shortName());
        entity.setCategory(req.category());
        entity.setDescription(req.description() != null ? req.description() : "");
        entity.setInstructions(req.instructions() != null ? req.instructions() : "");
        entity.setPurposes(req.purposes() != null && !req.purposes().isEmpty() ? req.purposes() : List.of(TestPurpose.SELF_DISCOVERY));
        
        // Isi default dan field yang sebelumnya terlewat
        entity.setVersion(version);
        entity.setStatus(TestStatus.DRAFT); // Selalu DRAFT
        entity.setScoringMode(req.scoringMode() != null ? req.scoringMode() : TestDefinition.ScoringMode.AUTO);
        entity.setScorerKey(req.scorerKey());
        entity.setScoringConfig(req.scoringConfig());
        entity.setResultViewType(req.resultViewType() != null ? req.resultViewType() : TestDefinition.ResultViewType.PROFILE);
        entity.setTotalItems(req.totalItems() != null ? req.totalItems() : 0);
        entity.setTotalDurationSeconds(req.totalDurationSeconds());
        entity.setEstimatedMinutes(req.estimatedMinutes() != null ? req.estimatedMinutes() : 0);
        entity.setPausePolicy(req.pausePolicy() != null ? req.pausePolicy() : TestDefinition.PausePolicy.FREE);
        entity.setDisconnectGraceSeconds(req.disconnectGraceSeconds() != null ? req.disconnectGraceSeconds() : 120);
        entity.setItemOrderPolicy(req.itemOrderPolicy() != null ? req.itemOrderPolicy() : TestDefinition.ItemOrderPolicy.FIXED);
        entity.setTargetAge(req.targetAge());
        entity.setMinEducation(req.minEducation());
        entity.setLanguage(req.language() != null ? req.language() : "id");
        entity.setRequiredFeature(req.requiredFeature());
        entity.setResultAccessLevel(req.resultAccessLevel() != null ? req.resultAccessLevel() : TestDefinition.ResultAccessLevel.STANDARD);
        entity.setConfig(req.config());
        entity.setLicense(req.license());
        entity.setThumbnailUrl(req.thumbnailUrl());
        entity.setTags(req.tags() != null ? req.tags() : List.of());
        
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        
        persist(entity);
        return entity;
    }

    public TestDefinition update(ObjectId id, TestDefinitionUpdateRequest req) {
        // Gunakan requireDraft untuk memfilter deletedAt, melempar TEST_NOT_FOUND, dan memastikan DRAFT
        TestDefinition entity = requireDraft(id);

        Instant now = Instant.now();

        entity.setName(req.name());
        entity.setShortName(req.shortName());
        entity.setCategory(req.category());
        entity.setDescription(req.description() != null ? req.description() : "");
        entity.setInstructions(req.instructions() != null ? req.instructions() : "");
        entity.setPurposes(req.purposes() != null && !req.purposes().isEmpty() ? req.purposes() : List.of(TestPurpose.SELF_DISCOVERY));
        
        // Tidak mengubah status dan code
        entity.setVersion(req.version() != null ? req.version() : "1.0.0");
        entity.setScoringMode(req.scoringMode() != null ? req.scoringMode() : TestDefinition.ScoringMode.AUTO);
        entity.setScorerKey(req.scorerKey());
        entity.setScoringConfig(req.scoringConfig());
        entity.setResultViewType(req.resultViewType() != null ? req.resultViewType() : TestDefinition.ResultViewType.PROFILE);
        entity.setTotalItems(req.totalItems() != null ? req.totalItems() : 0);
        entity.setTotalDurationSeconds(req.totalDurationSeconds());
        entity.setEstimatedMinutes(req.estimatedMinutes() != null ? req.estimatedMinutes() : 0);
        entity.setPausePolicy(req.pausePolicy() != null ? req.pausePolicy() : TestDefinition.PausePolicy.FREE);
        entity.setDisconnectGraceSeconds(req.disconnectGraceSeconds() != null ? req.disconnectGraceSeconds() : 120);
        entity.setItemOrderPolicy(req.itemOrderPolicy() != null ? req.itemOrderPolicy() : TestDefinition.ItemOrderPolicy.FIXED);
        entity.setTargetAge(req.targetAge());
        entity.setMinEducation(req.minEducation());
        entity.setLanguage(req.language() != null ? req.language() : "id");
        entity.setRequiredFeature(req.requiredFeature());
        entity.setResultAccessLevel(req.resultAccessLevel() != null ? req.resultAccessLevel() : TestDefinition.ResultAccessLevel.STANDARD);
        entity.setConfig(req.config());
        entity.setLicense(req.license());
        entity.setThumbnailUrl(req.thumbnailUrl());
        entity.setTags(req.tags() != null ? req.tags() : List.of());
        
        entity.setUpdatedAt(now);
        
        update(entity);
        return entity;
    }

    public TestDefinition requireDraft(ObjectId testId) {
        TestDefinition test = find("_id = ?1 and deletedAt is null", testId).firstResult();
        if (test == null) {
            throw new ValidationException("TEST_NOT_FOUND", "Tes tidak ditemukan");
        }
        if (test.getStatus() != TestStatus.DRAFT) {
            throw new ValidationException("TEST_IMMUTABLE", "Tes bukan DRAFT");
        }
        return test;
    }
    
    public TestDefinition publish(ObjectId testId) {
        TestDefinition test = find("_id = ?1 and deletedAt is null", testId).firstResult();
        if (test == null) {
            throw new ValidationException("TEST_NOT_FOUND", "Tes tidak ditemukan");
        }
        if (test.getStatus() != TestStatus.DRAFT) {
            throw new ValidationException("INVALID_STATE", "Hanya tes DRAFT yang bisa di-publish");
        }
        test.setStatus(TestStatus.ACTIVE);
        test.setUpdatedAt(Instant.now());
        update(test);
        return test;
    }

    public TestDefinition archive(ObjectId testId) {
        TestDefinition test = find("_id = ?1 and deletedAt is null", testId).firstResult();
        if (test == null) {
            throw new ValidationException("TEST_NOT_FOUND", "Tes tidak ditemukan");
        }
        if (test.getStatus() != TestStatus.ACTIVE) {
            throw new ValidationException("INVALID_STATE", "Hanya tes ACTIVE yang bisa di-archive");
        }
        test.setStatus(TestStatus.ARCHIVED);
        test.setUpdatedAt(Instant.now());
        update(test);
        return test;
    }
    
    public TestDefinition requireNotArchived(ObjectId testId) {
        TestDefinition test = find("_id = ?1 and deletedAt is null", testId).firstResult();
        if (test == null) {
            throw new ValidationException("TEST_NOT_FOUND", "Tes tidak ditemukan");
        }
        if (test.getStatus() == TestStatus.ARCHIVED) {
            throw new ValidationException("TEST_IMMUTABLE", "Tes sudah ARCHIVED");
        }
        return test;
    }
}
