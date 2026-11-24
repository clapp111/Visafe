import type { components } from "@/shared/api";
import { cn } from "@/shared/lib";
import type { ContractListLabels } from "../model/labels";

type Counts = components["schemas"]["ContractListResponseCounts"];
export type StatusFilterValue =
  components["schemas"]["ContractListResponseItem"]["status"] | undefined;

/** 상태 칩. 숫자는 필터와 관계없이 전체 기준이다. */
export function StatusFilter({
  counts,
  value,
  onChange,
  labels,
}: {
  counts?: Counts;
  value: StatusFilterValue;
  onChange: (value: StatusFilterValue) => void;
  labels: ContractListLabels;
}) {
  const chips: { value: StatusFilterValue; label: string; count?: number }[] = [
    { value: undefined, label: labels.filters.all, count: counts?.all },
    {
      value: "IN_PROGRESS",
      label: labels.filters.IN_PROGRESS,
      count: counts?.inProgress,
    },
    {
      value: "CONFIRMED",
      label: labels.filters.CONFIRMED,
      count: counts?.confirmed,
    },
    {
      value: "TERMINATED",
      label: labels.filters.TERMINATED,
      count: counts?.terminated,
    },
  ];

  return (
    <div
      role="group"
      aria-label={labels.filterLabel}
      className="mb-4 flex flex-wrap gap-2"
    >
      {chips.map((chip) => {
        const pressed = chip.value === value;
        return (
          <button
            key={chip.label}
            type="button"
            aria-pressed={pressed}
            onClick={() => onChange(chip.value)}
            className={cn(
              "h-10 cursor-pointer rounded-full border px-4 text-sm",
              pressed
                ? "border-primary bg-primary font-bold text-primary-foreground"
                : "border-border-strong bg-surface font-medium text-foreground hover:bg-surface-muted",
            )}
          >
            {chip.label} {chip.count ?? 0}
          </button>
        );
      })}
    </div>
  );
}
