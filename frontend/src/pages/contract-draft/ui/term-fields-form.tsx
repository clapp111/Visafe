"use client";

import { type FormEvent, useState } from "react";
import type { components } from "@/shared/api";
import { ArrowRightIcon, Button, Checkbox, FieldMessage } from "@/shared/ui";
import { FIELD_GROUPS } from "../model/field-groups";
import type { ContractDraftLabels } from "../model/labels";
import { TermFieldInput, type TermValue } from "./term-field-input";

type Field = components["schemas"]["ContractDraftResponseField"];

const CONFIRM_ID = "contract-draft-confirm";

export interface TermFieldsFormProps {
  fields: Field[];
  /** 근로자가 신고한 항목 키 (returned.reportedFields) */
  reportedKeys?: string[];
  /** 항목 키별 저장 실패 안내 */
  fieldErrors?: Record<string, string>;
  onCommitField: (key: string, value: TermValue) => void;
  onShowSource: (pageNo: number) => void;
  /** 원본과 같음을 체크하고 [제출하고 확인 요청 보내기]를 누르면 불린다 (제출 확인 창은 #61) */
  onSubmit: () => void;
  onReupload: () => void;
  /** 항목 저장 중이면 제출을 잠시 막는다 */
  isSaving?: boolean;
  labels: ContractDraftLabels;
}

/** 네 묶음으로 나눈 19개 항목, 원본 일치 확인, 제출 버튼 */
export function TermFieldsForm({
  fields,
  reportedKeys = [],
  fieldErrors = {},
  onCommitField,
  onShowSource,
  onSubmit,
  onReupload,
  isSaving,
  labels,
}: TermFieldsFormProps) {
  const [confirmed, setConfirmed] = useState(false);
  const [confirmError, setConfirmError] = useState(false);
  const byKey = new Map(fields.map((field) => [field.key, field]));

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!confirmed) {
      setConfirmError(true);
      return;
    }
    if (isSaving) return;
    onSubmit();
  }

  return (
    <form
      noValidate
      aria-labelledby="contract-draft-fields-title"
      onSubmit={handleSubmit}
      className="flex flex-col gap-10"
    >
      <h2 id="contract-draft-fields-title" className="sr-only">
        {labels.fieldsTitle}
      </h2>

      {FIELD_GROUPS.map((group) => (
        <section
          key={group.id}
          aria-labelledby={`contract-draft-${group.id}`}
          className="flex flex-col gap-5 border-t-2 border-foreground pt-6"
        >
          <h3
            id={`contract-draft-${group.id}`}
            className="text-xl leading-normal font-bold"
          >
            {labels.groups[group.id]}
          </h3>
          {group.id === "parties" && (
            <p className="-mt-2 text-sm text-muted">{labels.partiesHint}</p>
          )}
          {group.rows.map((row) => (
            <div key={row.join()} className="flex flex-wrap gap-x-6 gap-y-4">
              {row.map((key) => {
                const field = byKey.get(key);
                return (
                  field && (
                    <TermFieldInput
                      key={key}
                      field={field}
                      reported={reportedKeys.includes(key)}
                      errorMessage={fieldErrors[key]}
                      onCommit={onCommitField}
                      onShowSource={onShowSource}
                      labels={labels}
                    />
                  )
                );
              })}
            </div>
          ))}
        </section>
      ))}

      <div className="flex flex-col gap-4 rounded-lg border border-border bg-surface-muted p-6">
        <p className="font-bold">{labels.confirmNotice}</p>
        <Checkbox
          id={CONFIRM_ID}
          label={labels.confirmCheck}
          checked={confirmed}
          onChange={(event) => {
            setConfirmed(event.target.checked);
            if (event.target.checked) setConfirmError(false);
          }}
          aria-describedby={confirmError ? `${CONFIRM_ID}-message` : undefined}
        />
        {confirmError && (
          <FieldMessage id={`${CONFIRM_ID}-message`}>
            {labels.confirmRequired}
          </FieldMessage>
        )}
        <p className="text-sm text-muted">{labels.confirmHint}</p>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-4 border-t border-border pt-8">
        <Button variant="secondary" size="lg" onClick={onReupload}>
          {labels.reupload}
        </Button>
        <Button
          type="submit"
          size="lg"
          disabled={isSaving}
          className="px-8 text-lg"
        >
          {isSaving ? labels.saving : labels.submit}
          <ArrowRightIcon />
        </Button>
      </div>
    </form>
  );
}
