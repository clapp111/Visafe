package com.visafe.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CustomErrorCode implements ErrorCode {

    // JWT 인증 에러
    AUTH_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, 401, "이메일 또는 비밀번호가 올바르지 않습니다."),
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, 401, "인증이 필요합니다."),
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, 403, "접근 권한이 없습니다."),
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

    // 회원
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "회원을 찾을 수 없습니다."),

    // 계약 (볼 권한이 없는 계약도 CONTRACT_NOT_FOUND로 응답해 존재 여부를 숨긴다)
    CONTRACT_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "계약을 찾을 수 없습니다."),
    CONTRACT_STAGE_CONFLICT(HttpStatus.CONFLICT, 409, "지금 단계에서는 할 수 없는 작업입니다."),
    WORKER_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "근로자 계정을 찾을 수 없습니다."),
    PREVIOUS_CONTRACT_INVALID(HttpStatus.CONFLICT, 409, "대체할 수 없는 계약입니다. 같은 사업주와 근로자의 확정된 계약만 대체할 수 있습니다."),

    // 계약 원본 파일
    FILE_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, 413, "파일은 10MB까지 올릴 수 있습니다."),
    FILE_UNSUPPORTED_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, 415, "PDF, JPG, PNG 파일만 올릴 수 있습니다."),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "파일을 찾을 수 없습니다."),

    // 근로조건 항목 검토·제출
    TERM_KEY_UNKNOWN(HttpStatus.BAD_REQUEST, 400, "알 수 없는 항목입니다."),
    TERM_VALUE_INVALID(HttpStatus.BAD_REQUEST, 400, "항목 값이 올바르지 않습니다."),
    TERMS_REQUIRED_MISSING(HttpStatus.UNPROCESSABLE_CONTENT, 422, "비어 있는 필수 항목이 있습니다."),
    CONFIRMATION_REQUIRED(HttpStatus.BAD_REQUEST, 400, "원본과 같은지 확인했다는 체크가 필요합니다."),

    // 번역·근로자 확인
    TRANSLATION_NOT_READY(HttpStatus.CONFLICT, 409, "아직 번역이 끝나지 않았습니다."),
    TRANSLATION_OUTDATED(HttpStatus.CONFLICT, 409, "새 번역본이 나왔습니다. 계약 내용을 다시 확인해 주세요."),
    READ_ALL_REQUIRED(HttpStatus.BAD_REQUEST, 400, "모든 내용을 읽었다는 체크가 필요합니다."),
    REPORT_ITEMS_REQUIRED(HttpStatus.BAD_REQUEST, 400, "원본과 다른 항목을 하나 이상 골라 주세요."),

    // 계약 종료
    TERMINATION_ALREADY_ACTIVE(HttpStatus.CONFLICT, 409, "이미 진행 중인 종료 요청이 있습니다."),
    TERMINATION_DATE_INVALID(HttpStatus.BAD_REQUEST, 400, "종료일은 오늘 이후이고 계약 종료일 이전이어야 합니다."),
    TERMINATION_NOT_ALLOWED(HttpStatus.FORBIDDEN, 403, "이 종료 요청에 응답하거나 취소할 수 없습니다."),
    TERMINATION_STATUS_CONFLICT(HttpStatus.CONFLICT, 409, "이미 처리된 종료 요청입니다."),

    // 감독 기관 검토
    AUTHORITY_ALREADY_REVIEWED(HttpStatus.CONFLICT, 409, "이미 검토한 회차입니다."),
    REJECT_REASON_REQUIRED(HttpStatus.BAD_REQUEST, 400, "반려 사유를 입력해 주세요."),

    // 공개 조회
    LOOKUP_INVALID_FORMAT(HttpStatus.BAD_REQUEST, 400, "확인번호 형식이 올바르지 않습니다."),
    LOOKUP_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "확인번호에 해당하는 계약이 없습니다."),
    FILE_HASH_INVALID_FORMAT(HttpStatus.BAD_REQUEST, 400, "파일 해시 형식이 올바르지 않습니다."),

    // 공통 500 에러 코드
    COMMON_INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 500, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final Integer code;
    private final String message;
}
