import type { components } from "@/shared/api";

type Schemas = components["schemas"];

/** 확인 이력·블록체인 기록 문구. 화면 언어(#77)가 붙으면 언어별 값으로 바꾼다. */
export const CONTRACT_RECORDS_LABELS = {
  chainCaption: "블록체인 트랜잭션",
  chainRecord: "기록",
  chainTime: "일시",
  chainTxHash: "트랜잭션 해시",
  chainLink: "링크",
  chainExplorer: "탐색기에서 보기",
  recordTypes: {
    CONFIRM: "확정",
    TERMINATE: "종료",
  } satisfies Record<
    NonNullable<Schemas["ContractDetailResponseChainRecordItem"]["recordType"]>,
    string
  >,
  /** 블록에 포함되기 전이라 일시가 없을 때 일시 칸에 보인다 */
  chainPending: "기록 중",
  chainHint:
    "네트워크: Sepolia 테스트넷 · 기록 주체: Visafe 서버 지갑. 계약서 내용은 블록체인에 올라가지 않고 해시만 기록됩니다.",

  /** 조회(L1)는 공개 이벤트만, 상세(M2)는 모든 이벤트를 준다 */
  events: {
    CONTRACT_CREATED: "계약 등록",
    FILE_UPLOADED: "원본 올림",
    EXTRACTION_STARTED: "AI 항목 추출 시작",
    EXTRACTION_COMPLETED: "AI 항목 추출 완료",
    EXTRACTION_FAILED: "AI 항목 추출 실패",
    TERM_EDITED: "고용주 항목 수정",
    SUBMITTED: "고용주 제출",
    TRANSLATION_COMPLETED: "번역 완료",
    TRANSLATION_FAILED: "번역 실패",
    WORKER_UNDERSTOOD: "근로자 확인",
    WORKER_REPORTED: "근로자 신고 (원본과 다름)",
    AUTHORITY_REVIEW_REQUESTED: "감독 기관 확인 요청",
    AUTHORITY_APPROVED: "감독 기관 확인",
    AUTHORITY_REJECTED: "감독 기관 반려",
    CHAIN_RECORD_REQUESTED: "블록체인 기록 요청",
    CHAIN_RECORD_FAILED: "블록체인 기록 실패",
    CONFIRMED: "확정 · 확인번호 발급",
    TERMINATION_REQUESTED: "종료 요청",
    TERMINATION_AGREED: "종료 동의",
    TERMINATION_DECLINED: "종료 거절",
    TERMINATION_CANCELED: "종료 요청 취소",
    TERMINATED: "계약 종료",
  } satisfies Record<
    NonNullable<Schemas["ContractDetailResponseTimelineEntry"]["type"]>,
    string
  >,
  confirmedDetail: "블록체인 기록 완료",
  translationBasis: (language: string) => `${language} 번역본 기준`,
  reportReason: (reason: string) => `사유: ${reason}`,
  languageNames: { vi: "베트남어" } as Record<string, string>,
};

export type ContractRecordsLabels = typeof CONTRACT_RECORDS_LABELS;
