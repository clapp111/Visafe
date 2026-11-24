import type { components } from "@/shared/api";
import { formatDateTime } from "@/shared/lib";
import type { StepListItem } from "@/shared/ui";
import type { ContractReviewDoneLabels, ReviewDonePhase } from "./labels";

type Detail = components["schemas"]["ContractDetailResponse"];
type EventType = NonNullable<NonNullable<Detail["timeline"]>[number]["type"]>;

/** 확인 완료 화면이 다루는 stage. 그 밖의 stage는 다른 화면으로 보낸다. */
export function getReviewDonePhase(
  stage: Detail["stage"],
): ReviewDonePhase | undefined {
  if (stage === "RECORDING") return "recording";
  if (stage === "AUTHORITY_REVIEW") return "authority";
  if (stage === "DONE") return "done";
  return undefined;
}

function lastEvent(detail: Detail, type: EventType) {
  return detail.timeline?.findLast((entry) => entry.type === type);
}

/** M2 계약 상세로 진행 단계를 만든다. 기관 검토 계약이면 '감독 기관 확인' 단계가 들어간다. */
export function buildReviewDoneSteps(
  detail: Detail,
  phase: ReviewDonePhase,
  labels: ContractReviewDoneLabels,
): StepListItem[] {
  const { steps, stepStatuses } = labels;
  const at = (type: EventType) => {
    const occurredAt = lastEvent(detail, type)?.occurredAt;
    return occurredAt && formatDateTime(occurredAt);
  };
  const filled = (detail.terms ?? []).filter(
    (term) => term.value !== null && term.value !== undefined,
  ).length;
  const withAuthority =
    phase === "authority" || lastEvent(detail, "AUTHORITY_REVIEW_REQUESTED");
  const recordFailed =
    phase === "recording" &&
    detail.processing?.task === "CHAIN_RECORD" &&
    detail.processing.status === "FAILED";

  const confirmState =
    phase === "done"
      ? "done"
      : phase === "authority"
        ? "waiting"
        : recordFailed
          ? "failed"
          : "current";

  return [
    {
      id: "submitted",
      title: steps.submitted.title,
      description: steps.submitted.description(filled),
      state: "done",
      meta: at("SUBMITTED"),
    },
    {
      id: "translation",
      title: steps.translation.title,
      description: steps.translation.description,
      state: "done",
      meta: at("TRANSLATION_COMPLETED"),
    },
    {
      id: "worker",
      title: steps.worker.title,
      description: steps.worker.description(
        lastEvent(detail, "WORKER_UNDERSTOOD")?.actorLabel ??
          detail.worker?.name,
      ),
      state: "done",
      meta: at("WORKER_UNDERSTOOD"),
    },
    ...(withAuthority
      ? [
          {
            id: "authority",
            title: steps.authority.title,
            description:
              phase === "authority"
                ? steps.authority.current
                : steps.authority.done,
            state: phase === "authority" ? "current" : "done",
            meta:
              phase === "authority"
                ? stepStatuses.current
                : at("AUTHORITY_APPROVED"),
          } satisfies StepListItem,
        ]
      : []),
    {
      id: "confirm",
      title: steps.confirm.title,
      description: steps.confirm[confirmState],
      state: confirmState,
      meta:
        confirmState === "done" ? at("CONFIRMED") : stepStatuses[confirmState],
    },
  ];
}
