package com.psycorp.psychapi.feature.test.api.dto.request;

import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

import com.psycorp.psychapi.feature.test.models.TestItem.ItemStatus;
import com.psycorp.psychapi.feature.test.models.TestItem.ItemType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TestItemCreateRequest(
    // WAJIB. Id section di tes yang sama. Tidak bisa diubah setelah dibuat.
    @NotNull(message = "sectionId wajib diisi") ObjectId sectionId,

    // WAJIB. Integer >= 1, unik dalam satu section.
    @NotNull(message = "number wajib diisi") @Min(value = 1, message = "number minimal 1") Integer number,

    // WAJIB. Enum itemType.
    @NotNull(message = "type wajib diisi") ItemType type,

    // WAJIB. Object minimal punya "text" atau "images".
    @NotNull(message = "content wajib diisi") Map<String, Object> content,

    // OPSIONAL. Array opsi untuk tipe yang memerlukan pilihan.
    List<Map<String, Object>> options,

    // OPSIONAL. Id grup soal (soal yang berbagi stimulus).
    String groupId,

    // OPSIONAL. Id skala yang dinilai (wajib untuk LIKERT).
    ObjectId scaleId,

    // OPSIONAL. Default false. true = skor dibalik (soal negatif LIKERT, TRUE_FALSE, YES_NO).
    Boolean reverseScored,

    // OPSIONAL. Default 1.0.
    Double weight,

    // OPSIONAL. Default true. false = tidak masuk skor utama (MSAI soal 61-87).
    Boolean scoredInTotal,

    // OPSIONAL. Menautkan soal konsistensi EPPS dengan soal asalnya.
    String consistencyGroup,

    // WAJIB untuk tes ABILITY, null untuk tes lain.
    // { "correct": ["3"] } untuk pilihan, { "accepted": ["12"] } untuk isian.
    Map<String, Object> answerKey,

    // OPSIONAL. Batas waktu per soal (Wartegg per kotak).
    Integer timeLimitSeconds,

    // OPSIONAL. Aturan jawaban sesuai tabel 9.3 api.md.
    Map<String, Object> constraints,

    // OPSIONAL. Tingkat kesulitan hasil kalibrasi (0-1).
    Double difficulty,

    // OPSIONAL. Default false. true = soal contoh, tidak dihitung skor.
    Boolean isExample,

    // OPSIONAL. Default ACTIVE.
    ItemStatus status,

    // OPSIONAL. Pengelompokan tambahan.
    List<String> tags
) {}
