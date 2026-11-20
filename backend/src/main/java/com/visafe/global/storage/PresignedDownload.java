package com.visafe.global.storage;

import java.time.Instant;

/**
 * 원본 파일 Presigned GET URL.
 *
 * @param url 브라우저가 직접 여는 S3 주소 (서명 포함)
 * @param expiresAt URL 만료 시각
 */
public record PresignedDownload(String url, Instant expiresAt) {}
