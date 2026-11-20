package com.visafe.global.storage;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectLockMode;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

/** S3에 파일을 저장하는 {@link FileStorage} 구현. */
@Component
public class S3FileStorage implements FileStorage {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties properties;
    private final Clock clock;

    @Autowired
    public S3FileStorage(S3Client s3Client, S3Presigner s3Presigner, StorageProperties properties) {
        this(s3Client, s3Presigner, properties, Clock.systemUTC());
    }

    S3FileStorage(S3Client s3Client, S3Presigner s3Presigner, StorageProperties properties, Clock clock) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public String store(String key, byte[] content, String contentType) {
        String storageKey = toStorageKey(key);
        PutObjectRequest.Builder request = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(storageKey)
                .contentType(contentType)
                .contentLength((long) content.length);
        if (properties.objectLock().enabled()) {
            Instant retainUntil = clock.instant()
                    .atZone(ZoneOffset.UTC)
                    .plus(properties.objectLock().retention())
                    .toInstant();
            request.objectLockMode(ObjectLockMode.GOVERNANCE).objectLockRetainUntilDate(retainUntil);
        }
        try {
            s3Client.putObject(request.build(), RequestBody.fromBytes(content));
        } catch (SdkException e) {
            throw new FileStorageException("파일을 저장하지 못했습니다: " + storageKey, e);
        }
        return storageKey;
    }

    @Override
    public byte[] read(String storageKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(storageKey)
                .build();
        try {
            return s3Client.getObjectAsBytes(request).asByteArray();
        } catch (SdkException e) {
            throw new FileStorageException("파일을 읽지 못했습니다: " + storageKey, e);
        }
    }

    @Override
    public PresignedDownload presignDownload(String storageKey, String fileName, String contentType, boolean inline) {
        ContentDisposition.Builder disposition = inline ? ContentDisposition.inline() : ContentDisposition.attachment();
        GetObjectRequest getObject = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(storageKey)
                .responseContentType(contentType)
                .responseContentDisposition(disposition
                        .filename(fileName, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(properties.presignedUrlTtl())
                .getObjectRequest(getObject)
                .build();
        try {
            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
            return new PresignedDownload(presigned.url().toString(), presigned.expiration());
        } catch (SdkException e) {
            throw new FileStorageException("내려받기 URL을 만들지 못했습니다: " + storageKey, e);
        }
    }

    @Override
    public void delete(String storageKey) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(properties.bucket())
                .key(storageKey)
                .build();
        try {
            s3Client.deleteObject(request);
        } catch (SdkException e) {
            throw new FileStorageException("파일을 지우지 못했습니다: " + storageKey, e);
        }
    }

    private String toStorageKey(String key) {
        if (key == null || key.isBlank() || key.startsWith("/") || key.contains("..")) {
            throw new IllegalArgumentException("올바르지 않은 파일 경로입니다: " + key);
        }
        return properties.keyPrefix() + "/" + key;
    }
}
