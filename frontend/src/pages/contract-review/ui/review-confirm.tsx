"use client";

import { type FormEvent, useState } from "react";
import { Alert, Button, Checkbox, FieldMessage } from "@/shared/ui";
import type { ContractReviewLabels } from "../model/labels";

const CHECK_ID = "contract-review-read-all";

export interface ReviewConfirmProps {
  /** 모두 읽음을 체크하고 [이해했습니다]를 누르면 불린다 (W2 UNDERSTOOD) */
  onConfirm: () => void;
  /** [원본과 다릅니다] — 신고 창 열기 (#65) */
  onReport: () => void;
  isSubmitting?: boolean;
  /** 응답 실패 안내. onReload가 있으면 [최신 내용 불러오기]를 보인다 (TRANSLATION_OUTDATED) */
  error?: { message: string; onReload?: () => void };
  labels: ContractReviewLabels;
}

/** '내용을 이해했나요?' — 모두 읽음 체크와 응답 버튼 */
export function ReviewConfirm({
  onConfirm,
  onReport,
  isSubmitting,
  error,
  labels,
}: ReviewConfirmProps) {
  const [checked, setChecked] = useState(false);
  const [checkError, setCheckError] = useState(false);
  const [before, emphasis, after] = labels.confirmDescription;

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!checked) {
      setCheckError(true);
      return;
    }
    if (isSubmitting) return;
    onConfirm();
  }

  return (
    <form
      noValidate
      aria-labelledby="contract-review-confirm"
      onSubmit={handleSubmit}
      className="flex flex-col gap-5 rounded-lg border-2 border-primary p-7"
    >
      <h2 id="contract-review-confirm" className="text-2xl font-bold">
        {labels.confirmTitle}
      </h2>
      <p className="text-muted">
        {before}
        <strong className="text-foreground">{emphasis}</strong>
        {after}
      </p>
      <div className="flex flex-col gap-1">
        <Checkbox
          id={CHECK_ID}
          label={labels.confirmCheck}
          checked={checked}
          onChange={(event) => {
            setChecked(event.target.checked);
            if (event.target.checked) setCheckError(false);
          }}
          aria-describedby={checkError ? `${CHECK_ID}-message` : undefined}
        />
        {checkError && (
          <FieldMessage id={`${CHECK_ID}-message`}>
            {labels.confirmRequired}
          </FieldMessage>
        )}
      </div>

      {error && (
        <Alert
          tone="danger"
          title={labels.responseError}
          action={
            error.onReload && (
              <Button variant="secondary" size="md" onClick={error.onReload}>
                {labels.reload}
              </Button>
            )
          }
        >
          {error.message}
        </Alert>
      )}

      <div className="flex flex-wrap justify-between gap-4">
        <Button variant="secondary" onClick={onReport}>
          {labels.report}
        </Button>
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? labels.submitting : labels.understood}
        </Button>
      </div>
    </form>
  );
}
