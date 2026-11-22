package com.visafe.domain.term.enums;

/** 항목 값 타입 (API 응답의 fields[].type). 날짜는 2027-01-02, 시각은 09:00, 금액·숫자는 정수다. */
public enum TermValueType {
    STRING,
    DATE,
    TIME,
    INTEGER
}
