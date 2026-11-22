package com.visafe.domain.contract.dto.response;

import com.visafe.domain.contract.enums.ProcessingStatus;
import com.visafe.domain.contract.enums.ProcessingTask;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 진행 중인 작업. 프론트는 2~3초 간격으로 계약 상세를 다시 불러 이 값을 확인한다.
 */
@Schema(description = "진행 중인 작업 응답 DTO")
public record ProcessingResponse(
        @Schema(description = "작업 종류", example = "TRANSLATION")
        ProcessingTask task,

        @Schema(description = "진행 상태", example = "RUNNING") ProcessingStatus status,
        @Schema(description = "시도 횟수 (1부터)", example = "1") int attempt,
        @Schema(description = "마지막 오류", nullable = true) String lastError) {

    public static ProcessingResponse running(ProcessingTask task, int attempt) {
        return new ProcessingResponse(task, ProcessingStatus.RUNNING, attempt, null);
    }

    public static ProcessingResponse failed(ProcessingTask task, int attempt, String lastError) {
        return new ProcessingResponse(task, ProcessingStatus.FAILED, attempt, lastError);
    }
}
