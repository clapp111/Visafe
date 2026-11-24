import Link from "next/link";
import type { components } from "@/shared/api";
import { routes } from "@/shared/config";
import { cn, formatDateTime } from "@/shared/lib";
import {
  Badge,
  Button,
  CheckIcon,
  CopyButton,
  MinusCircleIcon,
  buttonClass,
} from "@/shared/ui";
import type { ContractDetailLabels, ContractRole } from "../model/labels";
import { stageActions } from "../model/stage-actions";

type Detail = components["schemas"]["ContractDetailResponse"];

export interface StatusCardProps {
  detail: Detail;
  contractId: number;
  role: ContractRole;
  /** [수정하기] (START_REVISION, R8) */
  onRevise: () => void;
  isRevising?: boolean;
  /** [등록된 원본 내려받기] (DOWNLOAD_FILE, M3) */
  onDownload: () => void;
  isDownloading?: boolean;
  labels: ContractDetailLabels;
}

/** 상태·단계 안내와 지금 할 수 있는 일 */
export function StatusCard({
  detail,
  contractId,
  role,
  onRevise,
  isRevising,
  onDownload,
  isDownloading,
  labels,
}: StatusCardProps) {
  const stage = detail.stage ?? "UPLOADED";
  const copy = labels.stages[stage];
  const description =
    typeof copy.description === "string"
      ? copy.description
      : copy.description[role];
  const confirmed = stage === "DONE" || stage === "ENDED";
  const canDownload =
    stage === "DONE" && detail.availableActions?.includes("DOWNLOAD_FILE");
  const actions = stageActions(detail, contractId, labels);

  return (
    <section
      aria-labelledby="contract-detail-status"
      className={cn(
        "mb-10 flex flex-wrap items-center justify-between gap-6 rounded-md border border-t-4 border-border px-7 py-6",
        stage === "DONE"
          ? "border-t-success"
          : stage === "ENDED"
            ? "border-t-border-strong"
            : "border-t-primary-hover",
      )}
    >
      <div className="flex flex-col gap-2">
        {detail.status && (
          <Badge
            tone={
              detail.status === "CONFIRMED"
                ? "success"
                : detail.status === "TERMINATED"
                  ? "neutral"
                  : "primary"
            }
            size="md"
            icon={
              detail.status === "CONFIRMED" ? (
                <CheckIcon size={16} />
              ) : detail.status === "TERMINATED" ? (
                <MinusCircleIcon size={16} />
              ) : undefined
            }
            className="self-start"
          >
            {labels.statuses[detail.status]}
          </Badge>
        )}
        <h2 id="contract-detail-status" className="text-2xl font-bold">
          {copy.title}
        </h2>
        {confirmed && detail.confirmedAt && detail.confirmationNo && (
          <span className="text-muted">
            {labels.confirmed(formatDateTime(detail.confirmedAt))}
            <strong className="font-mono text-foreground">
              {detail.confirmationNo}
            </strong>
          </span>
        )}
        {stage === "ENDED" && detail.terminatedAt && (
          <span className="text-muted">
            {labels.terminated(formatDateTime(detail.terminatedAt))}
          </span>
        )}
        {description && <p className="text-muted">{description}</p>}
      </div>

      <div className="flex flex-wrap gap-3">
        {confirmed && detail.confirmationNo && (
          <>
            {stage === "DONE" && (
              <CopyButton
                text={detail.confirmationNo}
                label={labels.copyConfirmation}
                copiedLabel={labels.copied}
              />
            )}
            <Link
              href={routes.lookup(detail.confirmationNo)}
              className={buttonClass({ variant: "secondary", size: "md" })}
            >
              {labels.lookup}
            </Link>
          </>
        )}
        {canDownload && (
          <Button size="md" disabled={isDownloading} onClick={onDownload}>
            {isDownloading ? labels.downloading : labels.download}
          </Button>
        )}
        {actions.map((action) =>
          action.kind === "link" ? (
            <Link
              key={action.id}
              href={action.href}
              className={buttonClass({ size: "md" })}
            >
              {action.label}
            </Link>
          ) : (
            <Button
              key={action.id}
              size="md"
              disabled={isRevising}
              onClick={onRevise}
            >
              {isRevising ? labels.revising : action.label}
            </Button>
          ),
        )}
      </div>
    </section>
  );
}
