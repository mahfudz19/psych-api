package com.psycorp.psychapi.feature.test.api.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.psycorp.psychapi.feature.test.models.TestItem;
import com.psycorp.psychapi.feature.test.models.TestItem.ItemStatus;
import com.psycorp.psychapi.feature.test.models.TestItem.ItemType;

public record TestItemResponse(
    String id,
    String testId,
    String testVersion,
    String sectionId,
    Integer number,
    ItemType type,
    Map<String, Object> content,
    List<Map<String, Object>> options,
    String groupId,
    String scaleId,
    Boolean reverseScored,
    Double weight,
    Boolean scoredInTotal,
    String consistencyGroup,
    Map<String, Object> answerKey,
    Integer timeLimitSeconds,
    Map<String, Object> constraints,
    Double difficulty,
    Boolean isExample,
    ItemStatus status,
    List<String> tags,
    Instant createdAt,
    Instant updatedAt
) {
    public static TestItemResponse fromEntity(TestItem item) {
        return new TestItemResponse(
            item.getId().toHexString(),
            item.getTestId() != null ? item.getTestId().toHexString() : null,
            item.getTestVersion(),
            item.getSectionId() != null ? item.getSectionId().toHexString() : null,
            item.getNumber(),
            item.getType(),
            item.getContent(),
            item.getOptions(),
            item.getGroupId(),
            item.getScaleId() != null ? item.getScaleId().toHexString() : null,
            item.getReverseScored(),
            item.getWeight(),
            item.getScoredInTotal(),
            item.getConsistencyGroup(),
            item.getAnswerKey(),
            item.getTimeLimitSeconds(),
            item.getConstraints(),
            item.getDifficulty(),
            item.getIsExample(),
            item.getStatus(),
            item.getTags(),
            item.getCreatedAt(),
            item.getUpdatedAt()
        );
    }
}
