export const routes = {
  home: "/",
  login: "/login",
  lookup: (confirmationNo: string) =>
    `/lookup/${encodeURIComponent(confirmationNo)}`,
  contracts: "/contracts",
  contractNew: "/contracts/new",
  /** 계약 등록 2단계 (추출 항목 검토·수정) */
  contractDraft: (contractId: number | string) =>
    `/contracts/new/${encodeURIComponent(contractId)}`,
  /** 계약 등록 제출 완료 (번역 진행 상황) */
  contractSubmitted: (contractId: number | string) =>
    `/contracts/new/${encodeURIComponent(contractId)}/done`,
  contract: (contractId: number | string) =>
    `/contracts/${encodeURIComponent(contractId)}`,
  /** 근로자 계약 내용 확인 (#64) */
  contractReview: (contractId: number | string) =>
    `/contracts/${encodeURIComponent(contractId)}/review`,
  /** 근로자 확인 완료 (#66) */
  contractReviewDone: (contractId: number | string) =>
    `/contracts/${encodeURIComponent(contractId)}/review/done`,
  adminContracts: "/admin/contracts",
  /** 기관 목록의 보기 (PENDING이 기본이라 쿼리 없이 연다) */
  adminContractsView: (
    view: "PENDING" | "APPROVED" | "REJECTED" | "TERMINATED",
  ) =>
    view === "PENDING" ? "/admin/contracts" : `/admin/contracts?view=${view}`,
  adminContract: (contractId: number | string) =>
    `/admin/contracts/${encodeURIComponent(contractId)}`,
} as const;
