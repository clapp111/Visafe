package com.visafe.global.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "에러 응답 DTO")
public class ErrorResponse {

    @Schema(description = "HTTP 상태 코드", example = "409")
    private final Integer code;

    @Schema(description = "에러 코드 이름. 같은 HTTP 상태 안에서 원인을 구분한다.", example = "CONTRACT_STAGE_CONFLICT")
    private final String errorCode;

    @Schema(description = "에러 메시지", example = "지금 단계에서는 할 수 없는 작업입니다.")
    private final String message;

    public static ErrorResponse of(ErrorCode errorCode) {
        return of(errorCode, errorCode.getMessage());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.getCode(), errorCode.name(), message);
    }
}
