package com.visafe.domain.contract.enums;

/** 백그라운드에서 오래 걸리는 작업 (응답의 processing.task). */
public enum ProcessingTask {
    /** AI 추출 (OCR + 항목 읽기), stage UPLOADED */
    EXTRACTION,
    /** 번역, stage TRANSLATING */
    TRANSLATION,
    /** 블록체인 기록, stage RECORDING */
    CHAIN_RECORD
}
