import type { ReactNode } from "react";
import { cn } from "@/shared/lib";
import { CheckIcon, ErrorIcon } from "../icons";

export type StepState = "done" | "current" | "failed" | "waiting";

export interface StepListItem {
  id: string;
  title: ReactNode;
  description?: ReactNode;
  state: StepState;
  /** 오른쪽 끝 글자 (끝난 시각, '진행 중' 등) */
  meta?: ReactNode;
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

/** 진행 단계 목록. 진행 중인 단계는 aria-current="step"으로 표시한다. */
export function StepList({ steps }: { steps: StepListItem[] }) {
  return (
    <ol className="border-t-2 border-foreground">
      {steps.map((step, index) => (
        <li
          key={step.id}
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
            {step.description && (
              <span className="block text-sm text-muted">
                {step.description}
              </span>
            )}
          </span>
          {step.meta && (
            <span
              className={cn(
                "text-sm whitespace-nowrap",
                step.state === "done" && "text-muted",
                step.state === "current" && "font-bold text-primary",
                step.state === "failed" && "font-bold text-danger",
              )}
            >
              {step.meta}
            </span>
          )}
        </li>
      ))}
    </ol>
  );
}
