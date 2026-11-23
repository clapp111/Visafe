export const routes = {
  home: "/",
  login: "/login",
  lookup: (confirmationNo: string) =>
    `/lookup/${encodeURIComponent(confirmationNo)}`,
  contracts: "/contracts",
  contractNew: "/contracts/new",
  contract: (contractId: number | string) =>
    `/contracts/${encodeURIComponent(contractId)}`,
  adminContracts: "/admin/contracts",
  /** 기관 목록의 보기 (PENDING이 기본이라 쿼리 없이 연다) */
  adminContractsView: (
    view: "PENDING" | "APPROVED" | "REJECTED" | "TERMINATED",
  ) =>
    view === "PENDING" ? "/admin/contracts" : `/admin/contracts?view=${view}`,
  adminContract: (contractId: number | string) =>
    `/admin/contracts/${encodeURIComponent(contractId)}`,
} as const;
