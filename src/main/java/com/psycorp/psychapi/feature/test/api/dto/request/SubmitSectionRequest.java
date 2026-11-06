package com.psycorp.psychapi.feature.test.api.dto.request;

import java.util.List;
import java.util.Map;

public record SubmitSectionRequest(
    // Opsional — jawaban terakhir yang belum sempat tersimpan
    List<Map<String, Object>> answers
) {}
