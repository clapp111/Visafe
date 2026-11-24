import type { components } from "@/shared/api";
import { routes } from "@/shared/config";
import type { ContractDetailLabels } from "./labels";

type Detail = components["schemas"]["ContractDetailResponse"];

export type StageAction =
  | { kind: "link"; id: string; href: string; label: string }
  | { kind: "revise"; id: string; label: string };

/**
 * 진행 중인 계약에서 할 수 있는 일(M2 availableActions)을 버튼으로 바꾼다.
 * 확정 계약의 복사·조회·내려받기와 종료(#69)는 여기서 다루지 않는다.
 */
export function stageActions(
  detail: Detail,
  contractId: number,
  labels: ContractDetailLabels,
): StageAction[] {
  const available = new Set(detail.availableActions ?? []);
  const actions: StageAction[] = [];

  if (available.has("EDIT_DRAFT")) {
    actions.push({
      kind: "link",
      id: "edit",
      href: routes.contractDraft(contractId),
      label: labels.editDraft,
    });
  }
  if (available.has("RETRY_EXTRACTION")) {
    actions.push({
      kind: "link",
      id: "retry-extraction",
      href: routes.contractDraft(contractId),
      label: labels.retry,
    });
  }
  if (available.has("RETRY_TRANSLATION")) {
    actions.push({
      kind: "link",
      id: "retry-translation",
      href: routes.contractSubmitted(contractId),
      label: labels.retry,
    });
  }
  if (available.has("RESPOND_REVIEW")) {
    actions.push({
      kind: "link",
      id: "review",
      href: routes.contractReview(contractId),
      label: labels.respondReview,
    });
  }
  if (available.has("START_REVISION")) {
    actions.push({ kind: "revise", id: "revise", label: labels.startRevision });
  }
  return actions;
}
