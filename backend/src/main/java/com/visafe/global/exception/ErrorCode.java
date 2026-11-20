package com.visafe.global.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

    /** 에러 코드 이름. 응답의 errorCode에 그대로 들어가며, enum이면 상수 이름이 쓰인다. */
    String name();

    HttpStatus getHttpStatus();

    Integer getCode();

    String getMessage();
}
