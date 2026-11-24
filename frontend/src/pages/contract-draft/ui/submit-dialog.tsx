"use client";

import type { components } from "@/shared/api";
import { Button, Dialog, FieldMessage, WarningIcon } from "@/shared/ui";
import type { ContractDraftLabels } from "../model/labels";

type Draft = components["schemas"]["ContractDraftResponse"];

export interface SubmitDialogProps {
  draft: Draft;
  onClose: () => void;
  /** [보내기] (R6 제출, confirmedOriginalMatch: true) */
  onConfirm: () => void;
  isSubmitting?: boolean;
  /** 제출 실패 안내 (409 CONTRACT_STAGE_CONFLICT 등). 422는 창을 닫고 빈 항목에 표시한다 */
  errorMessage?: string;
  labels: ContractDraftLabels;
}

/** 제출 확인 창. 받는 사람·확정할 항목 수·남은 경고를 보여 준다. */
export function SubmitDialog({
  draft,
  onClose,
  onConfirm,
  isSubmitting,
  errorMessage,
  labels,
}: SubmitDialogProps) {
  const fields = draft.fields ?? [];
  const filled = fields.filter(
    (field) => field.value !== null && field.value !== undefined,
  );
  const employerCount = fields.filter(
    (field) =>
      field.valueSource === "EMPLOYER_EDIT" ||
      field.valueSource === "EMPLOYER_INPUT",
  ).length;
  const warned = fields.filter((field) => field.warning);

  return (
    <Dialog
      open
      onClose={onClose}
      title={labels.submitTitle}
      description={labels.submitDescription}
      actions={
        <>
          <Button variant="secondary" size="md" onClick={onClose}>
            {labels.cancel}
          </Button>
          <Button size="md" disabled={isSubmitting} onClick={onConfirm}>
            {isSubmitting ? labels.sending : labels.send}
          </Button>
        </>
      }
    >
      <dl className="grid grid-cols-[120px_1fr] gap-x-4 gap-y-2.5 rounded-md bg-surface-muted px-5 py-4">
        <dt className="text-muted">{labels.recipient}</dt>
        <dd className="font-bold">{draft.worker?.name}</dd>
        <dt className="text-muted">{labels.termsToConfirm}</dt>
        <dd>{labels.termCount(filled.length, employerCount)}</dd>
      </dl>

      {warned.length > 0 && (
        <div className="flex items-start gap-2.5 rounded-md border border-warning-border bg-warning-subtle px-4 py-3.5 text-sm">
          <WarningIcon className="mt-0.5 shrink-0 text-warning" />
          <span>
            <strong>{labels.remainingWarnings(warned.length)}</strong>{" "}
            {labels.remainingWarningsHint(
              warned.map((field) => field.label).join(", "),
            )}
          </span>
        </div>
      )}

      <p className="text-sm text-muted">{labels.submitNote}</p>
      {errorMessage && <FieldMessage>{errorMessage}</FieldMessage>}
    </Dialog>
  );
}
