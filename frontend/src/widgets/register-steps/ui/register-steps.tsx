import { cn } from "@/shared/lib";
import { CheckIcon } from "@/shared/ui";
import { useCurrentLanguage } from "@/shared/i18n";
import { REGISTER_STEPS_LABELS } from "../model/labels";

/** 계약 등록 1·2단계 표시 */
export function RegisterSteps({
  current,
}: {
  /** 1부터 센 현재 단계 */
  current: number;
}) {
  const labels = REGISTER_STEPS_LABELS[useCurrentLanguage()];
  return (
    <ol
      aria-label={labels.label}
      className="mb-10 flex flex-wrap overflow-hidden rounded-md border border-border"
    >
      {labels.items.map((item, index) => {
        const step = index + 1;
        const isCurrent = step === current;
        const isDone = step < current;
        const status = isDone
          ? labels.done
          : isCurrent
            ? labels.current
            : labels.next;
        return (
          <li
            key={item}
            aria-current={isCurrent ? "step" : undefined}
            className={cn(
              "flex flex-[1_1_220px] items-center gap-3 border-b-4 px-5 py-4",
              index > 0 && "border-l border-l-border",
              isCurrent
                ? "border-b-primary bg-primary-subtle"
                : "border-b-transparent",
            )}
          >
            <span
              className={cn(
                "flex size-8 shrink-0 items-center justify-center rounded-full font-bold",
                isCurrent && "bg-primary text-primary-foreground",
                isDone && "border border-primary bg-surface text-primary",
                !isCurrent &&
                  !isDone &&
                  "border border-border-strong bg-surface text-muted",
              )}
            >
              {isDone ? <CheckIcon size={18} /> : step}
            </span>
            <span className="flex flex-col leading-[1.3]">
              <span
                className={cn(
                  "text-xs",
                  isCurrent ? "font-bold text-primary" : "text-muted",
                )}
              >
                {status}
              </span>
              <strong className={cn(!isCurrent && "text-muted")}>{item}</strong>
            </span>
          </li>
        );
      })}
    </ol>
  );
}
