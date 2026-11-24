import type { components } from "@/shared/api";

type Schemas = components["schemas"];

/** 조회 결과 화면 문구. 화면 언어(#54)가 붙으면 언어별 값으로 바꾼다. */
export const LOOKUP_RESULT_LABELS = {
  breadcrumb: "현재 위치",
  home: "홈",
  lookup: "계약 조회",
  title: "조회 결과",
  lookupForm: {
    label: "확인번호",
    submit: "다시 조회",
    required: "확인번호를 입력해 주세요.",
  },
  notFound: "입력한 확인번호로 등록된 계약이 없습니다.",
  invalidFormat: "확인번호 형식이 올바르지 않습니다.",

  confirmationNo: "확인번호",
  employer: "고용주",
  worker: "근로자",
  masked: "(일부 가림)",
  period: "계약기간",
  confirmedAt: "확정 일시",
  terminatedOn: "종료일",
  status: {
    confirmed: {
      badge: "확정",
      title: "현재 유효한 근로계약입니다",
    },
    terminated: {
      badge: "종료",
      title: "종료된 근로계약입니다",
      description:
        "더 이상 유효하지 않은 계약입니다. 확인 기록은 이력으로 남아 있습니다.",
    },
  },
  /** 확정 계약 설명. 확정 기록의 무결성(integrity.status)에 따라 바뀐다 */
  integrity: {
    VERIFIED: "근로자가 계약 내용을 확인했고, 확정 이후 바뀐 기록이 없습니다.",
    MISMATCH:
      "확정 기록이 블록체인에 기록된 값과 다릅니다. 계약서를 준 고용주에게 확인해 주세요.",
    PENDING:
      "근로자가 계약 내용을 확인했고, 블록체인 기록을 기다리는 중입니다.",
    UNAVAILABLE:
      "근로자가 계약 내용을 확인했습니다. 지금은 블록체인 기록과 대조할 수 없으니 잠시 후 다시 조회해 주세요.",
  } satisfies Record<
    NonNullable<Schemas["LookupResponseIntegrity"]["status"]>,
    string
  >,

  terminationReason: "종료 사유",
  terminationReasons: {
    EXPIRED: "계약 기간 만료",
    MUTUAL: "근로자·고용주 합의 종료",
    REPLACED: "새 계약으로 대체",
  } satisfies Record<
    NonNullable<Schemas["LookupResponseTermination"]["reasonType"]>,
    string
  >,
  terminationEndDate: (date: string) => `종료일은 ${date}입니다.`,
  terminationNewNumber:
    "근로조건이 바뀌어 새 계약을 맺었다면, 새로 발급된 확인번호로 조회하세요.",

  keyTermsTitle: "핵심 근로조건",
  keyTermsHint:
    "가지고 계신 계약서와 같은지 비교해 보세요. 임금을 포함한 전체 항목은 계약 당사자만 '내 계약'에서 볼 수 있습니다.",
  workHours: "근로시간",
  workDays: (days: number) => `주 ${days}일`,
  weeklySummary: (hours: number, breakMinutes: number) =>
    `주 ${hours}시간 · 휴게 ${breakMinutes}분`,
  noEndDate: "기간의 정함 없음",
  aboutYears: (years: number) => `약 ${years}년`,
  aboutMonths: (months: number) => `약 ${months}개월`,

  fileCheckTitle: "가지고 있는 계약서와 대조하기",
  fileCheckHint:
    "파일을 올리면 내용에서 해시(고유값)를 계산해 블록체인에 기록된 값과 비교합니다. 파일은 저장되지 않습니다.",
  fileCheckInput: "대조할 계약서 파일",
  noFile: "선택한 파일 없음",
  selectFile: "파일 선택",
  changeFile: "다른 파일 선택",
  check: "대조하기",
  checking: "대조하는 중",
  matchTitle: "일치 — 등록된 원본과 같은 파일입니다",
  matchDescription: "이 파일은 확정 이후 바뀌지 않았습니다.",
  mismatchTitle: "불일치 — 등록된 원본과 다른 파일입니다",
  mismatchDescription: "파일이 확정 이후 바뀌었거나, 다른 파일일 수 있습니다.",
  uploadedFileHash: "올린 파일 해시",
  recordedFileHash: "기록된 해시",
  mismatchTips: [
    "같은 계약서라도 다시 스캔하거나 사진을 찍은 파일은 값이 달라집니다. 등록할 때 올린 바로 그 파일이어야 합니다.",
    "등록된 파일과 같아야 하는데 다르다면 계약서가 바뀌었을 수 있습니다. 고용노동부 상담센터(국번 없이 1350)에 문의하세요.",
  ],

  chainTitle: "블록체인 기록",
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

  historyTitle: "확인 이력",
  /** 조회 API가 공개하는 이벤트(SUBMITTED, WORKER_UNDERSTOOD, AUTHORITY_APPROVED, CONFIRMED, TERMINATED)만 둔다 */
  historyEvents: {
    SUBMITTED: "고용주 제출",
    WORKER_UNDERSTOOD: "근로자 확인",
    AUTHORITY_APPROVED: "감독 기관 확인",
    CONFIRMED: "확정 · 확인번호 발급",
    TERMINATED: "계약 종료",
  } as Partial<
    Record<NonNullable<Schemas["LookupResponseHistoryEntry"]["type"]>, string>
  >,
  confirmedDetail: "블록체인 기록 완료",
  translationBasis: (language: string) => `${language} 번역본 기준`,
  languageNames: { vi: "베트남어" } as Record<string, string>,

  notFoundTitle: "확인번호를 다시 확인해 주세요",
  notFoundTips: {
    format: [
      "확인번호는 ",
      "VS-",
      "로 시작하는 12자리입니다. 숫자 0과 영문 O, 숫자 1과 영문 I는 헷갈리지 않도록 쓰지 않습니다.",
    ],
    confirmed: [
      "확인번호는 ",
      "계약이 확정된 뒤",
      "에 발급됩니다. 아직 근로자 확인 전인 계약은 조회되지 않습니다.",
    ],
    renewed: "근로조건이 바뀌어 새 계약을 맺었다면 확인번호도 새로 발급됩니다.",
  },
  notFoundHint:
    "계속 찾을 수 없다면 계약서를 준 고용주에게 확인번호를 다시 받아 보세요.",
};

export type LookupResultLabels = typeof LOOKUP_RESULT_LABELS;
