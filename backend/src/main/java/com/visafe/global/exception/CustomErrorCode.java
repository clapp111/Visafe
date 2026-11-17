package com.visafe.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CustomErrorCode implements ErrorCode {

    // JWT 인증 에러
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, 401, "인증이 필요합니다."),
    JWT_INVALID(HttpStatus.UNAUTHORIZED, 401, "유효하지 않은 토큰입니다."),
    JWT_EXPIRED(HttpStatus.UNAUTHORIZED, 401, "만료된 토큰입니다."),
    JWT_MALFORMED(HttpStatus.UNAUTHORIZED, 401, "잘못된 형식의 토큰입니다."),
    JWT_UNSUPPORTED(HttpStatus.UNAUTHORIZED, 401, "지원하지 않는 토큰입니다."),
    JWT_REFRESH_MISSING(HttpStatus.UNAUTHORIZED, 401, "리프레시 토큰이 없습니다."),
    JWT_REFRESH_NOT_MATCH(HttpStatus.BAD_REQUEST, 400, "리프레시 토큰이 일치하지 않습니다."),
    JWT_REFRESH_NOT_FOUND(HttpStatus.UNAUTHORIZED, 401, "리프레시 토큰을 찾을 수 없습니다."),

    // 요청 관련 에러
    COMMON_INVALID_REQUEST(HttpStatus.BAD_REQUEST, 400, "잘못된 요청입니다."),
    COMMON_INVALID_REQUEST_TYPE(HttpStatus.BAD_REQUEST, 400, "요청 타입이 잘못되었습니다"),
    COMMON_INVALID_PARAMETER(HttpStatus.BAD_REQUEST, 400, "요청 파라미터가 누락되었습니다."),
    COMMON_HANDLER_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "요청한 API를 찾을 수 없습니다."),
    COMMON_METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, 405, "지원하지 않는 HTTP 메서드입니다."),
    COMMON_UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, 415, "지원하지 않는 Content-Type입니다."),

    // 공통 500 에러 코드
    COMMON_INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 500, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final Integer code;
    private final String message;
}
