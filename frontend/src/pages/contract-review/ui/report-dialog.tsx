"use client";

import { useState } from "react";
import type { components } from "@/shared/api";
import {
  Badge,
  Button,
  Checkbox,
  Dialog,
  FieldMessage,
  Textarea,
} from "@/shared/ui";
import type { ContractReviewLabels } from "../model/labels";
import { REPORT_CHOICES } from "../model/report-choices";

type Item = components["schemas"]["ContractReviewResponseItem"];

const FORM_ID = "contract-review-report";

export interface ReportDialogProps {
  onClose: () => void;
  /** W1 항목. 값이 있는 항목의 선택지만 보이고, 고용주 입력이 섞이면 표시한다 */
  items: Item[];
  /** 고른 항목 키(묶음은 여러 개)와 사유로 불린다 (W2 REPORTED) */
  onSubmit: (report: { keys: string[]; reason?: string }) => void;
  isSubmitting?: boolean;
  /** 서버 안내 (REPORT_ITEMS_REQUIRED·TERM_KEY_UNKNOWN·TRANSLATION_OUTDATED 등) */
  errorMessage?: string;
  labels: ContractReviewLabels;
}

/** '원본과 다릅니다' 신고 창. 열 때 마운트해 고른 항목이 매번 비워진다. */
export function ReportDialog({
  onClose,
  items,
  onSubmit,
  isSubmitting,
  errorMessage,
  labels,
}: ReportDialogProps) {
  const [selected, setSelected] = useState<string[]>([]);
  const [reason, setReason] = useState("");
  const [itemsError, setItemsError] = useState(false);
  const byKey = new Map(items.map((item) => [item.key, item]));
  const choices = REPORT_CHOICES.map((choice) => {
    const keys = choice.keys.filter((key) => byKey.has(key));
    return {
      ...choice,
      keys,
      byEmployer: keys.some((key) => {
        const source = byKey.get(key)?.valueSource;
        return source !== undefined && source !== "AI";
      }),
    };
  }).filter((choice) => choice.keys.length > 0);

  function toggle(id: string, checked: boolean) {
    setSelected((current) =>
      checked ? [...current, id] : current.filter((value) => value !== id),
    );
    if (checked) setItemsError(false);
  }

  function handleSubmit() {
    if (selected.length === 0) {
      setItemsError(true);
      return;
    }
    if (isSubmitting) return;
    onSubmit({
      keys: choices
        .filter((choice) => selected.includes(choice.id))
        .flatMap((choice) => choice.keys),
      reason: reason.trim() || undefined,
    });
  }

  return (
    <Dialog
      open
      onClose={onClose}
      title={labels.reportTitle}
      description={labels.reportDescription}
      size="lg"
      actions={
        <>
          <Button variant="secondary" size="md" onClick={onClose}>
            {labels.cancel}
          </Button>
          <Button size="md" disabled={isSubmitting} onClick={handleSubmit}>
            {isSubmitting ? labels.submitting : labels.send}
          </Button>
        </>
      }
    >
      <fieldset
        aria-describedby={itemsError ? `${FORM_ID}-items-message` : undefined}
        className="flex flex-col gap-1"
      >
        <legend className="mb-2 font-bold">{labels.reportItemsLegend}</legend>
        <div className="grid gap-x-6 sm:grid-cols-2">
          {choices.map((choice) => (
            <Checkbox
              key={choice.id}
              checked={selected.includes(choice.id)}
              onChange={(event) => toggle(choice.id, event.target.checked)}
              label={
                <span className="inline-flex flex-wrap items-center gap-2">
                  {labels.reportChoices[choice.id]}
                  {choice.byEmployer && (
                    <Badge tone="highlight">{labels.employerInput}</Badge>
                  )}
                </span>
              }
            />
          ))}
        </div>
        {itemsError && (
          <FieldMessage id={`${FORM_ID}-items-message`}>
            {labels.reportItemsRequired}
          </FieldMessage>
        )}
      </fieldset>

      <Textarea
        id={`${FORM_ID}-reason`}
        label={
          <>
            {labels.reportReason}{" "}
            <span className="text-sm font-normal text-muted">
              {labels.reportReasonOptional}
            </span>
          </>
        }
        hint={labels.reportReasonHint}
        value={reason}
        onChange={(event) => setReason(event.target.value)}
      />

      {errorMessage && <FieldMessage>{errorMessage}</FieldMessage>}
    </Dialog>
  );
}
