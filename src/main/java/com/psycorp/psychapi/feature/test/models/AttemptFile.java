package com.psycorp.psychapi.feature.test.models;

import java.time.Instant;
import java.util.Map;
import org.bson.types.ObjectId;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection = "attempt_files")
public class AttemptFile extends PanacheMongoEntity {
    private ObjectId attemptId;
    private ObjectId itemId;
    private Integer boxNumber;
    private String storageKey;
    private String mimeType;
    private Long sizeBytes;
    private String checksum;
    private Map<String, Object> strokeData;
    private Instant uploadedAt = Instant.now();

    public ObjectId getAttemptId() { return attemptId; }
    public void setAttemptId(ObjectId attemptId) { this.attemptId = attemptId; }
    public ObjectId getItemId() { return itemId; }
    public void setItemId(ObjectId itemId) { this.itemId = itemId; }
    public Integer getBoxNumber() { return boxNumber; }
    public void setBoxNumber(Integer boxNumber) { this.boxNumber = boxNumber; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    public Map<String, Object> getStrokeData() { return strokeData; }
    public void setStrokeData(Map<String, Object> strokeData) { this.strokeData = strokeData; }
    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
}