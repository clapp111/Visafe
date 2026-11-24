import type { components } from "@/shared/api";

type ListResponse = components["schemas"]["ContractListResponse"];
type Item = components["schemas"]["ContractListResponseItem"];

// 화면 확인용 와이어프레임 예시값. M1 내 계약 목록을 연결하면 지운다.
const EMPLOYER_ITEMS: Item[] = [
  {
    contractId: 4833,
    contractNo: "C-2026-04833",
    counterpartyName: "Lê Thị Hoa",
    startDate: "2027-01-02",
    endDate: "2028-12-31",
    status: "IN_PROGRESS",
    stage: "RETURNED",
    todo: "FIX_REQUIRED",
  },
  {
    contractId: 5120,
    contractNo: "C-2026-05120",
    counterpartyName: "Nguyễn Văn An",
    startDate: "2027-01-02",
    endDate: "2029-12-31",
    status: "IN_PROGRESS",
    stage: "AWAITING_WORKER",
    todo: null,
  },
  {
    contractId: 4817,
    contractNo: "C-2026-04817",
    counterpartyName: "Nguyễn Văn An",
    startDate: "2027-01-02",
    endDate: "2029-12-31",
    status: "CONFIRMED",
    stage: "DONE",
    todo: null,
  },
  {
    contractId: 4650,
    contractNo: "C-2026-04650",
    counterpartyName: "Phạm Văn Đức",
    startDate: "2026-09-01",
    endDate: "2028-08-31",
    status: "CONFIRMED",
    stage: "DONE",
    todo: null,
  },
  {
    contractId: 412,
    contractNo: "C-2024-00412",
    counterpartyName: "Hoàng Văn Nam",
    startDate: "2024-03-04",
    endDate: "2026-03-03",
    status: "TERMINATED",
    stage: "ENDED",
    todo: null,
  },
];

const WORKER_ITEMS: Item[] = [
  {
    contractId: 5120,
    contractNo: "C-2026-05120",
    counterpartyName: "한빛정밀",
    startDate: "2027-01-02",
    endDate: "2029-12-31",
    status: "IN_PROGRESS",
    stage: "AWAITING_WORKER",
    todo: "CONFIRM_REQUIRED",
  },
  {
    contractId: 4817,
    contractNo: "C-2026-04817",
    counterpartyName: "한빛정밀",
    startDate: "2027-01-02",
    endDate: "2029-12-31",
    status: "CONFIRMED",
    stage: "DONE",
    todo: null,
  },
  {
    contractId: 981,
    contractNo: "C-2025-00981",
    counterpartyName: "서해수산",
    startDate: "2025-03-02",
    endDate: "2027-03-01",
    status: "TERMINATED",
    stage: "ENDED",
    todo: null,
  },
];

/**
 * 역할과 상태 필터로 예시 목록을 만든다. empty면 계약이 하나도 없는 상태를 보인다.
 * 숫자(counts·todoCount)는 서버처럼 필터와 관계없이 전체 기준으로 센다.
 */
export function getListFixture(
  role: "EMPLOYER" | "WORKER",
  status: Item["status"] | undefined,
  empty: boolean,
): ListResponse {
  const all = empty ? [] : role === "EMPLOYER" ? EMPLOYER_ITEMS : WORKER_ITEMS;
  const content = status ? all.filter((item) => item.status === status) : all;
  const count = (value: Item["status"]) =>
    all.filter((item) => item.status === value).length;
  return {
    counts: {
      all: all.length,
      inProgress: count("IN_PROGRESS"),
      confirmed: count("CONFIRMED"),
      terminated: count("TERMINATED"),
    },
    todoCount: all.filter((item) => item.todo).length,
    content,
    page: { number: 0, size: 20, totalElements: content.length, totalPages: 1 },
  };
}
