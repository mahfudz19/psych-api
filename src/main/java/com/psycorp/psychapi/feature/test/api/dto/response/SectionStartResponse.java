package com.psycorp.psychapi.feature.test.api.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record SectionStartResponse(
        SectionInfo section,
        List<Map<String, Object>> items,
        List<Map<String, Object>> answers,
        /** Hanya diisi ketika section sedang dalam fase hafalan (STIMULUS_IN_PROGRESS). */
        Map<String, Object> stimulusPhase
) {
    /** Convenience constructor tanpa stimulusPhase (fase tes normal). */
    public SectionStartResponse(SectionInfo section, List<Map<String, Object>> items,
            List<Map<String, Object>> answers) {
        this(section, items, answers, null);
    }

    public record SectionInfo(
            String id,
            String code,
            String name,
            Integer timeLimitSeconds,
            String navigationPolicy,
            Integer itemCount,
            Instant startedAt,
            Instant deadlineAt,
            Instant serverTime,
            Map<String, Object> config,
            List<Map<String, Object>> examples,
            Map<String, Object> stimulusPhase,
            String instructions,
            Instant stimulusStartedAt,
            Instant stimulusDeadlineAt
        ) {
    }
}
