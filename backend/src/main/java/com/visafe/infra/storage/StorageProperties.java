package com.visafe.infra.storage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Duration;
import java.time.Period;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * 파일 저장소 설정 (application.yml의 visafe.storage).
 * <p>
 * 버킷 하나를 환경별 키 접두사(local·dev·prod)로 나눠 쓴다. 로컬도 실제 S3에 저장한다.
 *
 * @param bucket 버킷 이름
 * @param region AWS 리전
 * @param keyPrefix 환경 접두사. 모든 객체 키 앞에 붙는다 (prod/contracts/31/…)
 * @param accessKey 로컬 개발용 IAM 사용자 키. 비우면 기본 자격 증명 체인(서버 IAM Role, ~/.aws 등)을 쓴다
 * @param secretKey accessKey와 함께 쓴다
 * @param presignedUrlTtl 원본 내려받기 URL 유효 시간
 * @param objectLock 객체 잠금 보존 설정
 */
@Validated
@ConfigurationProperties("visafe.storage")
public record StorageProperties(
        @NotBlank String bucket,
        @NotBlank String region,

        @NotBlank @Pattern(regexp = "^[a-z0-9-]+$", message = "키 접두사는 영어 소문자, 숫자, 하이픈만 쓸 수 있습니다.")
        String keyPrefix,

        String accessKey,
        String secretKey,
        @NotNull @DefaultValue("5m") Duration presignedUrlTtl,
        @Valid @NotNull @DefaultValue ObjectLock objectLock) {

    public boolean hasStaticCredentials() {
        return accessKey != null && !accessKey.isBlank();
    }

    /**
     * 객체 잠금 보존. 켜면 올리는 파일마다 Governance 모드로 보존 기간을 붙인다.
     * 버킷에 객체 잠금이 켜져 있어야 하며, 꺼진 버킷에 켜면 업로드가 실패한다.
     *
     * @param enabled 보존 기간을 붙일지 여부 (prod만 켠다)
     * @param retention 보존 기간 (근로계약서 법정 보존 3년)
     */
    public record ObjectLock(
            boolean enabled, @NotNull @DefaultValue("3y") Period retention) {}
}
