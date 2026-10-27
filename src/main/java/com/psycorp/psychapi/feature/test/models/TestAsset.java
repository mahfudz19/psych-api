package com.psycorp.psychapi.feature.test.models;

import org.bson.types.ObjectId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection = "test_assets")
public class TestAsset extends PanacheMongoEntity {
    private ObjectId testId;
    private AssetType type = AssetType.IMAGE;
    private String storageKey;
    private String url;
    private String mimeType;
    private Long sizeBytes;
    private String checksum;
    private Integer width;
    private Integer height;
    private String altText;

    public enum AssetType {
        IMAGE("IMAGE"), AUDIO("AUDIO");
        private final String v; AssetType(String v) { this.v = v; }
        @JsonValue public String getValue() { return v; }
        @JsonCreator public static AssetType fromValue(String v) {
            for (AssetType val : values()) if (val.v.equalsIgnoreCase(v)) return val;
            return null;
        }
    }

    public ObjectId getTestId() { return testId; }
    public void setTestId(ObjectId testId) { this.testId = testId; }
    public AssetType getType() { return type; }
    public void setType(AssetType type) { this.type = type; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    public String getAltText() { return altText; }
    public void setAltText(String altText) { this.altText = altText; }
}