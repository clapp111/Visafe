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
  adminContract: (contractId: number | string) =>
    `/admin/contracts/${encodeURIComponent(contractId)}`,
} as const;
