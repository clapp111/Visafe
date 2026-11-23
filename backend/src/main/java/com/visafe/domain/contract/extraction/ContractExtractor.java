package com.visafe.domain.contract.extraction;

/**
 * 계약서 원본에서 조항과 19개 근로조건 항목을 읽는다. LLM이 PDF·이미지를 직접 읽어 한 번에 돌려준다.
 * AI API 키가 있으면 Claude 구현을, 없으면 {@link StubContractExtractor}를 쓴다.
 */
public interface ContractExtractor {

    /**
     * 원본에서 조항과 항목을 읽는다.
     *
     * @param content 원본 파일 바이트
     * @param mimeType 원본 형식 (application/pdf, image/png, image/jpeg)
     * @return 읽은 조항과 항목. 읽지 못하면 예외를 던진다
     */
    ExtractionResult extract(byte[] content, String mimeType);
}
