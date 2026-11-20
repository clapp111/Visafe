package com.visafe.domain.contract.enums;

/**
 * contracts.stage — 세부 진행 단계 (저장 값).
 * <pre>
 * UPLOADED ⇄ REVIEWING → TRANSLATING → AWAITING_WORKER
 *   (REVIEWING → UPLOADED: 원본을 다시 올려 다시 추출)
 *   → (신고 시 RETURNED → REVIEWING)
 *   → (선택: AUTHORITY_REVIEW)
 *   → RECORDING → DONE → ENDED
 * </pre>
 */
public enum ContractStage {
    /** 원본 업로드 완료, AI 추출 대기·진행 */
    UPLOADED,
    /** 고용주가 2단계에서 항목 검토 중 */
    REVIEWING,
    /** 제출 후 번역 중 (실패 시 최대 3회 재시도) */
    TRANSLATING,
    /** 근로자 확인 대기 */
    AWAITING_WORKER,
    /** 근로자 신고로 고용주에게 돌아옴 */
    RETURNED,
    /** 기관 검토 중 (선택 단계) */
    AUTHORITY_REVIEW,
    /** 근로자 확인 후 블록체인 기록 중 (확인번호는 이 단계에 들어갈 때 정한다) */
    RECORDING,
    /** 기록 완료, 확인번호 발급 */
    DONE,
    /** 기간 만료·합의 종료·새 계약으로 대체 */
    ENDED;

    public boolean canTransitionTo(ContractStage next) {
        return switch (this) {
            case UPLOADED -> next == REVIEWING;
            case REVIEWING -> next == TRANSLATING || next == UPLOADED;
            case TRANSLATING -> next == AWAITING_WORKER;
            case AWAITING_WORKER -> next == RETURNED || next == AUTHORITY_REVIEW || next == RECORDING;
            case RETURNED -> next == REVIEWING;
            case AUTHORITY_REVIEW -> next == RECORDING || next == RETURNED;
            case RECORDING -> next == DONE;
            case DONE -> next == ENDED;
            case ENDED -> false;
        };
    }

    /** contracts.status 생성 컬럼과 같은 규칙. */
    public ContractStatus toStatus() {
        return switch (this) {
            case DONE -> ContractStatus.CONFIRMED;
            case ENDED -> ContractStatus.TERMINATED;
            default -> ContractStatus.IN_PROGRESS;
        };
    }
}
