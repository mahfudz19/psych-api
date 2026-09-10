package com.psycorp.psychapi.feature.storage.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import com.psycorp.psychapi.feature.storage.api.dto.response.UploadUrlResponse;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;

import io.quarkus.runtime.LaunchMode;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StorageService {

    @ConfigProperty(name = "gcs.project-id")
    String projectId;

    @ConfigProperty(name = "gcs.credentials-path", defaultValue = "")
    String credentialsPath;

    @ConfigProperty(name = "gcs.bucket.public")
    String publicBucket;

    @ConfigProperty(name = "gcs.bucket.private")
    String privateBucket;

    @ConfigProperty(name = "gcs.signed-url.expiry-minutes", defaultValue = "15")
    int expiryMinutes;

    @Inject
    LaunchMode launchMode;

    private Storage storage;

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        "image/jpeg", "image/png", "image/webp", "image/gif",
        "application/pdf"
    );

    private static final Map<String, String> CATEGORY_PATH = Map.of(
        "PROFILE_PICTURE", "profile",
        "DOCUMENT", "documents",
        "ORGANIZATION_LOGO", "org-logo"
    );

    @PostConstruct
    public void init() {
        try {
            StorageOptions.Builder builder = StorageOptions.newBuilder().setProjectId(projectId);
            if (launchMode == LaunchMode.NORMAL) {
                this.storage = builder.build().getService();
            } else {
                if (credentialsPath != null && !credentialsPath.isBlank()) {
                    builder.setCredentials(GoogleCredentials.fromStream(new FileInputStream(credentialsPath)));
                }
                this.storage = builder.build().getService();
            }
        } catch (IOException e) {
            throw new RuntimeException("GCS Init gagal — periksa credentials path: " + credentialsPath + " | " + e.getMessage(), e);
        }
    }

    public UploadUrlResponse generateUploadUrl(String userId, String filename, String mimeType, String category, String visibility) {
        // Validasi mime type
        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new ValidationException("INVALID_MIME_TYPE", "Tipe file tidak diizinkan: " + mimeType);
        }

        String categoryPath = CATEGORY_PATH.get(category);
        if (categoryPath == null) {
            throw new ValidationException("INVALID_CATEGORY", "Kategori tidak dikenal: " + category);
        }

        String bucket = "PUBLIC".equalsIgnoreCase(visibility) ? publicBucket : privateBucket;
        String extension = extractExtension(filename);
        String fileKey = "temp/users/" + userId + "/" + categoryPath + "/" + UUID.randomUUID() + extension;

        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucket, fileKey))
            .setContentType(mimeType)
            .build();

        URL signedUrl = storage.signUrl(
            blobInfo,
            expiryMinutes, TimeUnit.MINUTES,
            Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
            Storage.SignUrlOption.withContentType()
        );

        return new UploadUrlResponse(signedUrl.toString(), fileKey, bucket);
    }

    public String commitFile(String fileKey, String bucket) {
        try {

            BlobId sourceBlobId = BlobId.of(bucket, fileKey);
            if (storage.get(sourceBlobId) == null) {
                throw new ValidationException("FILE_NOT_FOUND", "File tidak ditemukan di staging area");
            }

            String permanentKey = fileKey.replaceFirst("^temp/", "");
            BlobId targetBlobId = BlobId.of(bucket, permanentKey);

            storage.copy(Storage.CopyRequest.of(sourceBlobId, targetBlobId));
            storage.delete(sourceBlobId);

            if (bucket.equals(publicBucket)) {
                return "https://storage.googleapis.com/" + bucket + "/" + permanentKey;
            }
            return permanentKey;
        } catch (ValidationException e) {
            throw e;
        } catch (StorageException e) {
            throw new ValidationException("GCS_ERROR", "Gagal memproses file di GCS: " + e.getMessage());
        }
    }

    public String commitPublicFile(String fileKey) {
        return this.commitFile(fileKey, this.publicBucket);
    }
    
    public String commitPrivateFile(String fileKey) {
        return this.commitFile(fileKey, this.privateBucket);
    }

    public void deleteFile(String fileKey, String bucket) {
        BlobId blobId = BlobId.of(bucket, fileKey);
        if (storage.get(blobId) != null) {
            storage.delete(blobId);
        }    }

    public void deletePublicFile(String fileUrlOrKey) {
        if (fileUrlOrKey == null || fileUrlOrKey.isBlank()) return;
        String fileKey = extractFileKey(fileUrlOrKey, this.publicBucket);
        this.deleteFile(fileKey, this.publicBucket);
    }

    public void deletePrivateFile(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) return;
        this.deleteFile(fileKey, this.privateBucket);    }

    private String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }

    private String extractFileKey(String path, String bucket) {
        String prefix = "https://storage.googleapis.com/" + bucket + "/";
        if (path.startsWith(prefix)) {
            return path.substring(prefix.length());
        }
        return path;
    }
}