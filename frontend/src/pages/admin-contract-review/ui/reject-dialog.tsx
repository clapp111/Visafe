"use client";

import { useState } from "react";
import { Button, Dialog, FieldMessage, Textarea } from "@/shared/ui";
import type { AdminContractReviewLabels } from "../model/labels";

/** 반려 사유 창 (D4). 사유는 필수다. 열 때 마운트해 입력이 매번 비워진다 */
export function RejectDialog({
  onClose,
  onSubmit,
  isSubmitting,
  errorMessage,
  labels,
}: {
  onClose: () => void;
  /** 반려 사유로 불린다 (D4 reject) */
  onSubmit: (reason: string) => void;
  isSubmitting?: boolean;
  /** 서버 안내 (REJECT_REASON_REQUIRED·AUTHORITY_ALREADY_REVIEWED 등) */
  errorMessage?: string;
  labels: AdminContractReviewLabels;
}) {
  const [reason, setReason] = useState("");
  const [required, setRequired] = useState(false);

  function handleSubmit() {
    if (!reason.trim()) {
      setRequired(true);
      return;
    }
    if (isSubmitting) return;
    onSubmit(reason.trim());
  }

  return (
    <Dialog
      open
      onClose={onClose}
      title={labels.rejectTitle}
      description={labels.rejectDescription}
      size="lg"
      actions={
        <>
          <Button variant="secondary" size="md" onClick={onClose}>
            {labels.cancel}
          </Button>
          <Button
            variant="danger"
            size="md"
            disabled={isSubmitting}
            onClick={handleSubmit}
          >
            {isSubmitting ? labels.rejecting : labels.rejectSubmit}
          </Button>
        </>
      }
    >
      <Textarea
        id="admin-reject-reason"
        label={labels.rejectReason}
        required
        hint={labels.rejectReasonHint}
        value={reason}
        onChange={(event) => {
          setReason(event.target.value);
          if (event.target.value.trim()) setRequired(false);
        }}
        message={required ? labels.rejectReasonRequired : undefined}
        status={required ? "error" : undefined}
      />
      {errorMessage && <FieldMessage>{errorMessage}</FieldMessage>}
    </Dialog>
  );
}
