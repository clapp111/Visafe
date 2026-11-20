package com.visafe.global.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectLockMode;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

class S3FileStorageTest {

    private static final Instant NOW = Instant.parse("2026-11-20T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final S3Client s3Client = mock(S3Client.class);
    // 서명은 네트워크 없이 로컬에서 계산되므로 실제 서명기를 쓴다.
    private final S3Presigner presigner = S3Presigner.builder()
            .region(Region.AP_NORTHEAST_2)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("AKIATEST", "secret")))
            .build();

    @AfterEach
    void tearDown() {
        presigner.close();
    }

    @Test
    void storeAddsEnvironmentPrefixAndKeepsContentType() {
        S3FileStorage storage = storage(false);
        byte[] content = {1, 2, 3};

        String storageKey = storage.store("contracts/31/abc.pdf", content, "application/pdf");

        assertThat(storageKey).isEqualTo("prod/contracts/31/abc.pdf");
        PutObjectRequest request = capturePut();
        assertThat(request.bucket()).isEqualTo("visafe-bucket");
        assertThat(request.key()).isEqualTo("prod/contracts/31/abc.pdf");
        assertThat(request.contentType()).isEqualTo("application/pdf");
        assertThat(request.contentLength()).isEqualTo(3L);
        assertThat(request.objectLockMode()).isNull();
        assertThat(request.objectLockRetainUntilDate()).isNull();
    }

    @Test
    void storeAddsGovernanceRetentionWhenObjectLockEnabled() {
        S3FileStorage storage = storage(true);

        storage.store("contracts/31/abc.pdf", new byte[] {1}, "application/pdf");

        PutObjectRequest request = capturePut();
        assertThat(request.objectLockMode()).isEqualTo(ObjectLockMode.GOVERNANCE);
        assertThat(request.objectLockRetainUntilDate()).isEqualTo(Instant.parse("2029-11-20T00:00:00Z"));
    }

    @Test
    void storeRejectsKeysThatEscapeThePrefix() {
        S3FileStorage storage = storage(false);

        for (String key : new String[] {"", " ", "/contracts/1.pdf", "../dev/contracts/1.pdf"}) {
            assertThatThrownBy(() -> storage.store(key, new byte[] {1}, "application/pdf"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void storeWrapsSdkFailure() {
        S3FileStorage storage = storage(false);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkClientException.create("connection refused"));

        assertThatThrownBy(() -> storage.store("contracts/31/abc.pdf", new byte[] {1}, "application/pdf"))
                .isInstanceOf(FileStorageException.class)
                .hasCauseInstanceOf(SdkClientException.class);
    }

    @Test
    void presignDownloadSignsContentTypeAndKoreanFileName() {
        S3FileStorage storage = storage(false);

        PresignedDownload download =
                storage.presignDownload("prod/contracts/31/abc.pdf", "표준근로계약서.pdf", "application/pdf", false);

        String url = URLDecoder.decode(download.url(), StandardCharsets.UTF_8);
        assertThat(url).contains("visafe-bucket").contains("prod/contracts/31/abc.pdf");
        assertThat(url).contains("X-Amz-Expires=300");
        assertThat(url).contains("response-content-type=application/pdf");
        // 브라우저는 filename*(UTF-8)을 쓰고, 그 앞의 filename="…"은 오래된 클라이언트용 ASCII 대체 이름이다.
        assertThat(url).contains("response-content-disposition=attachment;");
        assertThat(url)
                .contains("filename*=UTF-8''%ED%91%9C%EC%A4%80%EA%B7%BC%EB%A1%9C%EA%B3%84%EC%95%BD%EC%84%9C.pdf");
        assertThat(download.expiresAt())
                .isBetween(Instant.now().plusSeconds(290), Instant.now().plusSeconds(310));
    }

    @Test
    void presignDownloadUsesInlineForPreview() {
        S3FileStorage storage = storage(false);

        PresignedDownload download =
                storage.presignDownload("prod/contracts/31/abc.pdf", "contract.pdf", "application/pdf", true);

        assertThat(URLDecoder.decode(download.url(), StandardCharsets.UTF_8))
                .contains("response-content-disposition=inline;");
    }

    private PutObjectRequest capturePut() {
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        return captor.getValue();
    }

    private S3FileStorage storage(boolean objectLockEnabled) {
        StorageProperties properties = new StorageProperties(
                "visafe-bucket",
                "ap-northeast-2",
                "prod",
                null,
                null,
                Duration.ofMinutes(5),
                new StorageProperties.ObjectLock(objectLockEnabled, Period.ofYears(3)));
        return new S3FileStorage(s3Client, presigner, properties, CLOCK);
    }
}
