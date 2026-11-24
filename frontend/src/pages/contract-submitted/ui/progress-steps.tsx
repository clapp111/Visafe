import type { components } from "@/shared/api";
import { cn, formatDateTime } from "@/shared/lib";
import { CheckIcon, ErrorIcon } from "@/shared/ui";
import type { ContractSubmittedLabels } from "../model/labels";

type Detail = components["schemas"]["ContractDetailResponse"];
type StepState = "done" | "current" | "failed" | "waiting";

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

function StepMark({ state, index }: { state: StepState; index: number }) {
  const base =
    "flex size-8 shrink-0 items-center justify-center rounded-full font-bold";
  if (state === "done") {
    return (
      <span className={cn(base, "bg-primary-hover text-primary-foreground")}>
        <CheckIcon size={18} strokeWidth={2.2} />
      </span>
    );
  }
  if (state === "current") {
    return (
      <span
        aria-hidden="true"
        className="size-8 shrink-0 animate-spin rounded-full border-3 border-primary-border border-t-primary"
      />
    );
  }
  if (state === "failed") {
    return (
      <span className={cn(base, "bg-danger-subtle text-danger")}>
        <ErrorIcon size={20} />
      </span>
    );
  }
  return (
    <span className={cn(base, "border border-border-strong")}>{index + 1}</span>
  );
}

/** 제출 이후 진행 상황 (고용주 제출 → 번역 → 근로자 확인 → 확정) */
export function ProgressSteps({
  detail,
  labels,
}: {
  detail: Detail;
  labels: ContractSubmittedLabels;
}) {
  return (
    <ol className="border-t-2 border-foreground">
      {buildSteps(detail, labels).map((step, index) => (
        <li
          key={step.title}
          aria-current={step.state === "current" ? "step" : undefined}
          className={cn(
            "flex items-center gap-4 border-b border-divider px-2 py-4",
            step.state === "current" && "bg-primary-subtle",
            step.state === "waiting" && "text-muted",
          )}
        >
          <StepMark state={step.state} index={index} />
          <span className="flex-auto">
            <strong
              className={cn(
                step.state === "current" && "text-primary",
                step.state === "failed" && "text-danger",
              )}
            >
              {step.title}
            </strong>
            <span className="block text-sm text-muted">{step.description}</span>
          </span>
          <span
            className={cn(
              "text-sm whitespace-nowrap",
              step.state === "done" && "text-muted",
              step.state === "current" && "font-bold text-primary",
              step.state === "failed" && "font-bold text-danger",
            )}
          >
            {step.state === "done"
              ? step.at && formatDateTime(step.at)
              : labels.statuses[step.state]}
          </span>
        </li>
      ))}
    </ol>
  );
}
