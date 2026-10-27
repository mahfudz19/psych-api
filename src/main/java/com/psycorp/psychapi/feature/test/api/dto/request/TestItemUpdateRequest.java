package com.psycorp.psychapi.feature.test.api.dto.request;

import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

import com.psycorp.psychapi.feature.test.models.TestItem.ItemStatus;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TestItemUpdateRequest(
    // WAJIB. Nomor soal (unik per section).
    @NotNull(message = "number wajib diisi") @Min(value = 1, message = "number minimal 1") Integer number,

    // WAJIB. Minimal punya "text" atau "images".
    @NotNull(message = "content wajib diisi") Map<String, Object> content,

    // Opsi (sesuai tipe soal).
    List<Map<String, Object>> options,

    // Id grup soal.
    String groupId,

    // Id skala yang dinilai.
    ObjectId scaleId,

    // Apakah skor dibalik.
    Boolean reverseScored,

    // Bobot soal.
    Double weight,

    // Apakah masuk skor utama.
    Boolean scoredInTotal,

    // Grup konsistensi EPPS.
    String consistencyGroup,

    // Kunci jawaban (ABILITY) atau null.
    Map<String, Object> answerKey,

    // Batas waktu per soal.
    Integer timeLimitSeconds,

    // Aturan jawaban.
    Map<String, Object> constraints,

    // Tingkat kesulitan (0-1).
    Double difficulty,

    // Apakah soal contoh.
    Boolean isExample,

    // Status soal.
    ItemStatus status,

    // Tag.
    List<String> tags

    // CATATAN: sectionId dan type TIDAK ada di sini — tidak boleh diubah setelah dibuat.
) {}
