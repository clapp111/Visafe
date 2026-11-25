import { ADMIN_CONTRACTS_LABELS, type AdminView } from "./labels";

const VIEWS = ["PENDING", "APPROVED", "REJECTED", "TERMINATED"] as const;

/** 주소의 ?view= 값. 모르는 값이면 대기 목록 */
export function parseAdminView(value: string | null | undefined): AdminView {
  return VIEWS.find((view) => view === value) ?? "PENDING";
}

/** 보기별 제목 (화면 제목과 브라우저 탭 제목) */
export function adminViewTitle(value: string | null | undefined) {
  return ADMIN_CONTRACTS_LABELS.ko.titles[parseAdminView(value)];
}
