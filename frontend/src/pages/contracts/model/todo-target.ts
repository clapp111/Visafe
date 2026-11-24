import type { components } from "@/shared/api";
import { routes } from "@/shared/config";

type Item = components["schemas"]["ContractListResponseItem"];

/** 행의 버튼·계약 번호를 눌렀을 때 갈 곳. 수정 필요(FIX_REQUIRED)는 R8로 새 회차를 만든 뒤 2단계로 간다. */
export type TodoTarget = { kind: "link"; href: string } | { kind: "revise" };

export function todoTarget(item: Item): TodoTarget {
  const id = item.contractId ?? 0;
  switch (item.todo) {
    case "FIX_REQUIRED":
      return { kind: "revise" };
    case "REVIEW_INCOMPLETE":
    case "EXTRACTION_FAILED":
      return { kind: "link", href: routes.contractDraft(id) };
    case "TRANSLATION_FAILED":
      return { kind: "link", href: routes.contractSubmitted(id) };
    case "CONFIRM_REQUIRED":
      return { kind: "link", href: routes.contractReview(id) };
    default:
      return { kind: "link", href: routes.contract(id) };
  }
}
