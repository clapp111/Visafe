"use client";

import { useState } from "react";
import { formatDate } from "@/shared/lib";
import { Button, Dialog, FieldMessage, TextField, Textarea } from "@/shared/ui";
import type { ContractDetailLabels } from "../model/labels";

const FORM_ID = "contract-detail-termination-request";

export interface TerminationRequestDialogProps {
  onClose: () => void;
  /** 마지막 근무일(YYYY-MM-DD)과 사유로 불린다 (M4) */
  onSubmit: (request: { endDate: string; reasonText?: string }) => void;
  isSubmitting?: boolean;
  /** 서버 안내 (TERMINATION_DATE_INVALID·TERMINATION_ALREADY_ACTIVE 등) */
  errorMessage?: string;
  /** 고를 수 있는 첫날 (오늘, 한국 시간) */
  minDate: string;
  /** 고를 수 있는 마지막 날 (계약 종료일, 없으면 제한 없음) */
  maxDate?: string;
  labels: ContractDetailLabels;
}

/** 종료 요청 창. 열 때 마운트해 입력이 매번 비워진다. */
export function TerminationRequestDialog({
  onClose,
  onSubmit,
  isSubmitting,
  errorMessage,
  minDate,
  maxDate,
  labels,
}: TerminationRequestDialogProps) {
  const t = labels.termination;
  const [endDate, setEndDate] = useState("");
  const [reason, setReason] = useState("");
  const [dateError, setDateError] = useState<string>();
  const range = maxDate
    ? `${formatDate(minDate)} ~ ${formatDate(maxDate)}`
    : `${formatDate(minDate)} ~`;

  function handleSubmit() {
    if (!endDate) {
      setDateError(t.lastWorkDayRequired);
      return;
    }
    // YYYY-MM-DD는 글자 순서가 날짜 순서와 같다
    if (endDate < minDate || (maxDate && endDate > maxDate)) {
      setDateError(t.lastWorkDayInvalid);
      return;
    }
    if (isSubmitting) return;
    onSubmit({ endDate, reasonText: reason.trim() || undefined });
  }

  return (
    <Dialog
      open
      onClose={onClose}
      title={t.dialogTitle}
      description={t.dialogDescription}
      actions={
        <>
          <Button variant="secondary" size="md" onClick={onClose}>
            {t.close}
          </Button>
          <Button size="md" disabled={isSubmitting} onClick={handleSubmit}>
            {isSubmitting ? t.sending : t.send}
          </Button>
        </>
      }
    >
      <TextField
        id={`${FORM_ID}-date`}
        type="date"
        label={t.lastWorkDay}
        required
        min={minDate}
        max={maxDate}
        hint={t.lastWorkDayHint(range)}
        value={endDate}
        onChange={(event) => {
          setEndDate(event.target.value);
          setDateError(undefined);
        }}
        message={dateError}
        status={dateError ? "error" : undefined}
      />
      <Textarea
        id={`${FORM_ID}-reason`}
        label={
          <>
            {t.reason}{" "}
            <span className="text-sm font-normal text-muted">
              {t.reasonOptional}
            </span>
          </>
        }
        hint={t.reasonHint}
        value={reason}
        onChange={(event) => setReason(event.target.value)}
      />
      {errorMessage && <FieldMessage>{errorMessage}</FieldMessage>}
    </Dialog>
  );
}
