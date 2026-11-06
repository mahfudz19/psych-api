package com.psycorp.psychapi.feature.test.api.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record SectionStartResponse(
    SectionInfo section,
    List<Map<String, Object>> items,   // soal tanpa kunci jawaban
    List<Map<String, Object>> answers  // jawaban yang sudah tersimpan
) {
    public record SectionInfo(
        String id,
        String code,
        String name,
        String instructions,
        List<Map<String, Object>> examples,
        Boolean showExamplesFirst,
        Integer timeLimitSeconds,
        String navigationPolicy,
        Integer itemCount,
        Instant startedAt,
        Instant deadlineAt,
        Instant serverTime
    ) {}
}
