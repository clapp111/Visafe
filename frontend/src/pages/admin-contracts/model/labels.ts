export type AdminView = "PENDING" | "APPROVED" | "REJECTED" | "TERMINATED";

/** 감독 기관 목록 문구. 화면 언어(#77)가 붙으면 언어별 값으로 바꾼다. */
export const ADMIN_CONTRACTS_LABELS = {
  breadcrumb: "현재 위치",
  home: "홈",
  loading: "계약 목록을 불러오는 중",
  loadError: "계약 목록을 불러오지 못했습니다",

  titles: {
    PENDING: "확인 대기 목록",
    APPROVED: "확정한 계약",
    REJECTED: "반려한 계약",
    TERMINATED: "종료된 계약",
  } satisfies Record<AdminView, string>,
  descriptions: {
    PENDING:
      "근로자 확인까지 끝난 계약입니다. 내용을 검토한 뒤 확정하거나 반려하세요.",
    APPROVED: "기관이 확정한 계약입니다.",
    REJECTED: "기관이 반려해 고용주에게 돌려보낸 계약입니다.",
    TERMINATED: "기관이 확정한 뒤 종료된 계약입니다.",
  } satisfies Record<AdminView, string>,

  searchLabel: "계약 번호 · 고용주 · 근로자 검색",
  searchPlaceholder: "예: C-2026-04817",
  search: "검색",
  total: (count: number) => `전체 ${count}건`,
  sortOrder: "근로자 확인이 오래된 순",
  legendWarning: "확인 필요",
  legendWarningHint: "법 기준 등 경고가 남은 항목",
  legendEmployer: "고용주 입력",
  legendEmployerHint: "고용주가 직접 입력·수정한 항목",

  caption: (title: string) => `${title} 계약 목록`,
  columns: {
    contractNo: "계약 번호",
    employer: "고용주",
    worker: "근로자",
    language: "언어",
    workerConfirmedAt: "근로자 확인 일시",
    warningCount: "확인 필요",
    employerInputCount: "고용주 입력",
    action: "검토",
  },
  count: (count: number) => `${count}건`,
  review: "검토",
  view: "보기",
  languageNames: { vi: "베트남어" } as Record<string, string>,

  empty: {
    PENDING: "확인을 기다리는 계약이 없습니다",
    APPROVED: "확정한 계약이 없습니다",
    REJECTED: "반려한 계약이 없습니다",
    TERMINATED: "종료된 계약이 없습니다",
  } satisfies Record<AdminView, string>,
  emptySearch: "검색 결과가 없습니다",
  emptySearchHint: "계약 번호나 이름을 다시 확인해 주세요.",

  infoTitle: "감독 기관 확인이란?",
  info: [
    "확정하면 계약 내용의 해시가 블록체인(Sepolia 테스트넷)에 기록되고 확인번호가 발급됩니다.",
    "번역 내용은 이 단계에서 검토하지 않습니다. 번역은 근로자 확인과 '원본과 다릅니다' 신고로 검증합니다.",
  ],

  pagination: "목록 쪽 이동",
  previousPage: "이전",
  nextPage: "다음",
  pageStatus: (page: number, total: number) => `${page} / ${total}쪽`,
};

export type AdminContractsLabels = typeof ADMIN_CONTRACTS_LABELS;
