import type { components } from "@/shared/api";
import { formatDateTime } from "@/shared/lib";
import { StepList, type StepState } from "@/shared/ui";
import { useCurrentLanguage } from "@/shared/i18n";
import type { ContractSubmittedLabels } from "../model/labels";

type Detail = components["schemas"]["ContractDetailResponse"];

interface Step {
  title: string;
  description: string;
  state: StepState;
  /** 끝난 단계의 시각 */
  at?: string;
}

function lastEventAt(detail: Detail, type: string) {
  return detail.timeline?.findLast((entry) => entry.type === type)?.occurredAt;
}

/** M2 계약 상세로 네 단계의 상태를 정한다. 번역이 끝나면(AWAITING_WORKER) 근로자 확인이 진행 중이 된다. */
function buildSteps(detail: Detail, labels: ContractSubmittedLabels): Step[] {
  const { steps } = labels;
  const translating = detail.stage === "TRANSLATING";
  const failed = translating && detail.processing?.status === "FAILED";
  const filled = (detail.terms ?? []).filter(
    (term) => term.value !== null && term.value !== undefined,
  ).length;

  return [
    {
      title: steps.submitted.title,
      description: steps.submitted.description(filled),
      state: "done",
      at: lastEventAt(detail, "SUBMITTED"),
    },
    {
      title: steps.translation.title,
      description: failed
        ? steps.translation.failed
        : translating
          ? steps.translation.description
          : steps.translation.done,
      state: failed ? "failed" : translating ? "current" : "done",
      at: translating
        ? undefined
        : lastEventAt(detail, "TRANSLATION_COMPLETED"),
    },
    {
      title: steps.worker.title,
      description: translating
        ? steps.worker.description
        : steps.worker.current,
      state: translating ? "waiting" : "current",
    },
    {
      title: steps.confirm.title,
      description: steps.confirm.description,
      state: "waiting",
    },
  ];
}

/** 제출 이후 진행 상황 (고용주 제출 → 번역 → 근로자 확인 → 확정) */
export function ProgressSteps({
  detail,
  labels,
}: {
  detail: Detail;
  labels: ContractSubmittedLabels;
}) {
  const language = useCurrentLanguage();
  return (
    <StepList
      steps={buildSteps(detail, labels).map((step) => ({
        id: step.title,
        title: step.title,
        description: step.description,
        state: step.state,
        meta:
          step.state === "done"
            ? step.at && formatDateTime(step.at, language)
            : labels.statuses[step.state],
      }))}
    />
  );
}
