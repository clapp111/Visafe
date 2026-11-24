import type { components } from "@/shared/api";
import { cn, formatDateTime } from "@/shared/lib";
import { Badge, CheckIcon, MinusCircleIcon } from "@/shared/ui";
import { formatDate, formatPeriod } from "../lib/format";
import type { LookupResultLabels } from "../model/labels";

type LookupResponse = components["schemas"]["LookupResponse"];

export interface StatusCardProps {
  result: LookupResponse;
  labels: LookupResultLabels;
}

/** 확정·종료 상태와 고용주·근로자·계약기간을 보이는 맨 위 카드 */
export function StatusCard({ result, labels }: StatusCardProps) {
  const isTerminated = result.status === "TERMINATED";
  const status = isTerminated
    ? labels.status.terminated
    : labels.status.confirmed;
  const description = isTerminated
    ? labels.status.terminated.description
    : labels.integrity[result.integrity?.status ?? "PENDING"];
  const terminatedOn = result.termination?.endDate;
  // 마지막 칸: 종료된 계약은 종료일, 확정된 계약은 확정 일시
  const lastValue = isTerminated
    ? terminatedOn && formatDate(terminatedOn)
    : result.confirmedAt && formatDateTime(result.confirmedAt);

  return (
    <section
      aria-labelledby="lookup-status-title"
      className={cn(
        "flex flex-col gap-5 rounded-md border border-t-4 border-border px-8 py-7",
        isTerminated ? "border-t-border-strong" : "border-t-success",
      )}
    >
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="flex flex-col gap-2.5">
          <Badge
            tone={isTerminated ? "neutral" : "success"}
            size="md"
            icon={
              isTerminated ? (
                <MinusCircleIcon size={16} />
              ) : (
                <CheckIcon size={16} />
              )
            }
            className="self-start"
          >
            {status.badge}
          </Badge>
          <h2 id="lookup-status-title" className="text-3xl font-bold">
            {status.title}
          </h2>
          <p className="text-muted">{description}</p>
        </div>
        <div className="flex flex-col items-end gap-0.5">
          <span className="text-sm text-muted">{labels.confirmationNo}</span>
          <strong className="font-mono text-2xl tracking-[1px]">
            {result.confirmationNo}
          </strong>
        </div>
      </div>

      <dl className="grid grid-cols-2 gap-x-6 gap-y-4 border-t border-divider pt-5 lg:grid-cols-4">
        <div className="flex flex-col gap-0.5">
          <dt className="text-sm text-muted">{labels.employer}</dt>
          <dd className="font-bold">{result.companyName}</dd>
        </div>
        <div className="flex flex-col gap-0.5">
          <dt className="text-sm text-muted">{labels.worker}</dt>
          <dd className="font-bold">
            {result.workerNameMasked}{" "}
            <span className="text-xs font-normal text-muted">
              {labels.masked}
            </span>
          </dd>
        </div>
        <div className="flex flex-col gap-0.5">
          <dt className="text-sm text-muted">{labels.period}</dt>
          <dd className="font-bold">
            {result.startDate && formatPeriod(result.startDate, result.endDate)}
          </dd>
        </div>
        <div className="flex flex-col gap-0.5">
          <dt className="text-sm text-muted">
            {isTerminated ? labels.terminatedOn : labels.confirmedAt}
          </dt>
          <dd className="font-bold">{lastValue}</dd>
        </div>
      </dl>
    </section>
  );
}
