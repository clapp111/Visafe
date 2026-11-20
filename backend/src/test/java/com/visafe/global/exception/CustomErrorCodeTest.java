package com.visafe.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CustomErrorCodeTest {

    /** 응답의 code는 HTTP 상태 코드와 같아야 한다. (enum에 두 값을 따로 적으므로 어긋나지 않게 검사) */
    @ParameterizedTest
    @EnumSource(CustomErrorCode.class)
    void codeMatchesHttpStatus(CustomErrorCode errorCode) {
        assertThat(errorCode.getCode()).isEqualTo(errorCode.getHttpStatus().value());
    }

    @ParameterizedTest
    @EnumSource(CustomErrorCode.class)
    void messageIsNotBlank(CustomErrorCode errorCode) {
        assertThat(errorCode.getMessage()).isNotBlank();
    }
}
