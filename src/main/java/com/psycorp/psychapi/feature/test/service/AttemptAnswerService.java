package com.psycorp.psychapi.feature.test.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

import com.psycorp.psychapi.feature.test.models.AttemptAnswer;
import com.psycorp.psychapi.feature.test.models.AttemptAnswer.AnswerStatus;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AttemptAnswerService implements PanacheMongoRepository<AttemptAnswer> {

    /** Ambil dokumen jawaban untuk satu section, atau null jika belum ada. */
    public AttemptAnswer findBySection(ObjectId attemptId, ObjectId sectionId) {
        return find("attemptId = ?1 and sectionId = ?2", attemptId, sectionId).firstResult();
    }

    /** Buat dokumen jawaban kosong saat section dimulai. */
    public AttemptAnswer create(ObjectId attemptId, ObjectId sectionId, ObjectId testId, ObjectId userId) {
        Instant now = Instant.now();
        AttemptAnswer doc = new AttemptAnswer();
        doc.setAttemptId(attemptId);
        doc.setSectionId(sectionId);
        doc.setTestId(testId);
        doc.setUserId(userId);
        doc.setAnswers(new ArrayList<>());
        doc.setStatus(AnswerStatus.OPEN);
        doc.setRevision(0);
        doc.setSavedAt(now);
        doc.setCreatedAt(now);
        doc.setUpdatedAt(now);
        persist(doc);
        return doc;
    }

    /**
     * Upsert jawaban per itemId. Response null menghapus jawaban itemId tersebut.
     * Mengembalikan jumlah item yang diproses.
     */
    public int upsertAnswers(AttemptAnswer doc, List<Map<String, Object>> incoming) {
        if (incoming == null || incoming.isEmpty()) return 0;

        List<Map<String, Object>> existing = doc.getAnswers() != null
                ? new ArrayList<>(doc.getAnswers())
                : new ArrayList<>();

        for (Map<String, Object> inAnswer : incoming) {
            Object rawItemId = inAnswer.get("itemId");
            if (rawItemId == null) continue;
            String itemId = rawItemId.toString();

            existing.removeIf(a -> itemId.equals(String.valueOf(a.get("itemId"))));

            // null response = hapus jawaban
            if (inAnswer.get("response") != null) {
                Map<String, Object> entry = new java.util.HashMap<>(inAnswer);
                entry.put("answeredAt", Instant.now().toString());
                existing.add(entry);
            }
        }

        doc.setAnswers(existing);
        doc.setRevision(doc.getRevision() + 1);
        doc.setSavedAt(Instant.now());
        doc.setUpdatedAt(Instant.now());
        update(doc);
        return incoming.size();
    }

    public void close(AttemptAnswer doc, AnswerStatus closeStatus) {
        doc.setStatus(closeStatus);
        doc.setUpdatedAt(Instant.now());
        update(doc);
    }

    public long countAnswered(ObjectId attemptId, ObjectId sectionId) {
        AttemptAnswer doc = findBySection(attemptId, sectionId);
        if (doc == null || doc.getAnswers() == null) return 0;
        return doc.getAnswers().stream()
                .filter(a -> a.get("response") != null)
                .count();
    }

    public void autoCloseExpired(ObjectId attemptId, List<ObjectId> sectionIds) {
        for (ObjectId sid : sectionIds) {
            AttemptAnswer doc = findBySection(attemptId, sid);
            if (doc != null && doc.getStatus() == AnswerStatus.OPEN) {
                close(doc, AnswerStatus.AUTO_CLOSED);
            }
        }
    }
}
