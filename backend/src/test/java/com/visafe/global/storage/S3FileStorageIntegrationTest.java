package com.visafe.global.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.visafe.global.config.S3Config;
import com.visafe.global.crypto.Keccak256;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Period;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * 실제 S3로 저장 → 읽기 → Presigned URL 내려받기 → 삭제를 확인한다. local/it-test/ 아래에 썼다가 지운다.
 * 환경 변수 S3_IT_BUCKET, S3_IT_ACCESS_KEY, S3_IT_SECRET_KEY가 있을 때만 실행한다. (CI에서는 건너뜀)
 */
@EnabledIfEnvironmentVariable(named = "S3_IT_BUCKET", matches = ".+")
class S3FileStorageIntegrationTest {

    /** "표준근로계약서.pdf"의 RFC 5987 인코딩 */
    private static final String KOREAN_FILE_NAME_ENCODED =
            "%ED%91%9C%EC%A4%80%EA%B7%BC%EB%A1%9C%EA%B3%84%EC%95%BD%EC%84%9C.pdf";

    private S3Client s3Client;
    private S3Presigner presigner;
    private S3FileStorage storage;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties(
                System.getenv("S3_IT_BUCKET"),
                env("S3_IT_REGION", "ap-northeast-2"),
                "local",
                System.getenv("S3_IT_ACCESS_KEY"),
                System.getenv("S3_IT_SECRET_KEY"),
                Duration.ofMinutes(5),
                new StorageProperties.ObjectLock(false, Period.ofYears(3)));
        S3Config config = new S3Config();
        s3Client = config.s3Client(properties);
        presigner = config.s3Presigner(properties);
        storage = new S3FileStorage(s3Client, presigner, properties);
    }

    @AfterEach
    void tearDown() {
        s3Client.close();
        presigner.close();
    }

    @Test
    void storedBytesComeBackUnchangedThroughPresignedUrl() throws Exception {
        byte[] content = ("%PDF-1.7 계약서 " + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
        String key = "it-test/" + UUID.randomUUID() + ".pdf";

        String storageKey = storage.store(key, content, "application/pdf");

        assertThat(storageKey).isEqualTo("local/" + key);
        assertThat(storage.read(storageKey)).isEqualTo(content);

        PresignedDownload download = storage.presignDownload(storageKey, "표준근로계약서.pdf", "application/pdf", false);
        HttpResponse<byte[]> response = HttpClient.newHttpClient()
                .send(
                        HttpRequest.newBuilder(URI.create(download.url())).build(),
                        HttpResponse.BodyHandlers.ofByteArray());

        assertThat(response.statusCode()).isEqualTo(200);
        // 내려받은 바이트의 해시가 저장한 원본과 같아야 공개 조회 파일 대조가 맞는다.
        assertThat(Keccak256.hash(response.body())).isEqualTo(Keccak256.hash(content));
        assertThat(response.headers().firstValue("Content-Type")).hasValue("application/pdf");
        assertThat(response.headers().firstValue("Content-Disposition"))
                .hasValueSatisfying(value -> assertThat(value)
                        .startsWith("attachment;")
                        .contains("filename*=UTF-8''" + KOREAN_FILE_NAME_ENCODED));

        storage.delete(storageKey);

        assertThatThrownBy(() -> storage.read(storageKey)).isInstanceOf(FileStorageException.class);
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
