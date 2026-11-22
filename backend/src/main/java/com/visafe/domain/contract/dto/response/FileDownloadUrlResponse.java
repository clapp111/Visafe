package com.visafe.domain.contract.dto.response;

import com.visafe.domain.contract.entity.ContractFile;
import com.visafe.global.storage.PresignedDownload;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "원본 파일 URL. 브라우저가 S3에서 직접 받는다.")
public record FileDownloadUrlResponse(
        @Schema(description = "S3 Presigned GET URL") String url,

        @Schema(description = "URL 만료 시각", example = "2026-11-13T05:11:00Z")
        Instant expiresAt,

        @Schema(description = "파일 이름", example = "원본_근로계약서.pdf")
        String fileName,

        @Schema(description = "파일 형식", example = "application/pdf")
        String mimeType,

        @Schema(description = "크기(바이트)", example = "1258291")
        long sizeBytes) {

    public static FileDownloadUrlResponse of(ContractFile file, PresignedDownload download) {
        return new FileDownloadUrlResponse(
                download.url(), download.expiresAt(), file.getFileName(), file.getMimeType(), file.getSizeBytes());
    }
}
