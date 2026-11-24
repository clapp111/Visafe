import type { components } from "@/shared/api";
import { Badge, Button, Dialog, FieldMessage } from "@/shared/ui";
import type { AdminContractReviewLabels } from "../model/labels";

type Detail = components["schemas"]["AdminContractDetailResponse"];

/** 확정 확인 창 (D3). 남은 경고는 이력에 남는다는 것을 알린다 */
export function ApproveDialog({
  detail,
  onClose,
  onConfirm,
  isSubmitting,
  errorMessage,
  labels,
}: {
  detail: Detail;
  onClose: () => void;
  /** [확정] (D3 approve) */
  onConfirm: () => void;
  isSubmitting?: boolean;
  errorMessage?: string;
  labels: AdminContractReviewLabels;
}) {
  const warned = (detail.fields ?? []).filter((field) => field.warning);
  return (
    <Dialog
      open
      onClose={onClose}
      title={labels.approveTitle}
      size="lg"
      actions={
        <>
          <Button variant="secondary" size="md" onClick={onClose}>
            {labels.cancel}
          </Button>
          <Button size="md" disabled={isSubmitting} onClick={onConfirm}>
            {isSubmitting ? labels.approving : labels.approve}
          </Button>
        </>
      }
    >
      <dl className="grid grid-cols-[120px_1fr] gap-x-4 gap-y-2.5 rounded-md bg-surface-muted px-6 py-5">
        <dt className="text-muted">{labels.summary.contractNo}</dt>
        <dd className="font-bold">{detail.contractNo}</dd>
        <dt className="text-muted">{labels.summary.employer}</dt>
        <dd className="font-bold">{detail.employer?.companyName}</dd>
        <dt className="text-muted">{labels.summary.worker}</dt>
        <dd lang="vi" className="font-bold">
          {detail.worker?.name}
        </dd>
        <dt className="text-muted">{labels.remainingWarnings}</dt>
        <dd>
          {warned.length > 0 ? (
            <>
              <Badge tone="warning">{labels.warningTag}</Badge>{" "}
              {labels.warningsValue(
                warned.map((field) => field.label).join(", "),
              )}
            </>
          ) : (
            labels.noWarnings
          )}
        </dd>
      </dl>
      <ul className="flex list-disc flex-col gap-1.5 pl-5">
        {labels.approveNotes.map((note) => (
          <li key={note}>{note}</li>
        ))}
      </ul>
      {errorMessage && <FieldMessage>{errorMessage}</FieldMessage>}
    </Dialog>
  );
}
