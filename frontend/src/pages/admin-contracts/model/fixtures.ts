import type { components } from "@/shared/api";
import type { AdminView } from "./labels";

type ListResponse = components["schemas"]["AdminContractListResponse"];
type Item = components["schemas"]["AdminContractListResponseItem"];

/** 화면 확인용 D1 응답 (와이어프레임 예시값). API 연동 단계에서 지운다. */
const ITEMS: Item[] = [
  {
    contractId: 4,
    contractNo: "C-2026-04817",
    companyName: "한빛정밀",
    workerName: "Nguyễn Văn An",
    language: "vi",
    workerConfirmedAt: "2026-11-13T05:05:00Z",
    warningCount: 1,
    employerInputCount: 2,
  },
  {
    contractId: 3,
    contractNo: "C-2026-04790",
    companyName: "대성식품",
    workerName: "Trần Thị Mai",
    language: "vi",
    workerConfirmedAt: "2026-11-13T02:20:00Z",
    warningCount: 0,
    employerInputCount: 0,
  },
  {
    contractId: 1,
    contractNo: "C-2026-04752",
    companyName: "서해수산",
    workerName: "Lê Văn Hùng",
    language: "vi",
    workerConfirmedAt: "2026-11-12T08:48:00Z",
    warningCount: 0,
    employerInputCount: 1,
  },
];

/** ?empty=1 이면 빈 목록. 검색어는 계약 번호·사업체명·근로자 이름에서 찾는다 */
export function getAdminListFixture(
  view: AdminView,
  q: string,
  empty: boolean,
): ListResponse {
  const keyword = q.trim().toLowerCase();
  const content =
    empty || view === "TERMINATED"
      ? []
      : ITEMS.filter(
          (item) =>
            !keyword ||
            [item.contractNo, item.companyName, item.workerName].some((text) =>
              text?.toLowerCase().includes(keyword),
            ),
        );
  return {
    content,
    page: {
      number: 0,
      size: 20,
      totalElements: content.length,
      totalPages: 1,
    },
  };
}
