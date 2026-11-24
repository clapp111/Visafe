"use client";

import type { FocusEvent } from "react";
import type { components } from "@/shared/api";
import { cn } from "@/shared/lib";
import {
  Badge,
  FieldMessage,
  WarningIcon,
  fieldControlClass,
} from "@/shared/ui";
import type { ContractDraftLabels } from "../model/labels";

type Field = components["schemas"]["ContractDraftResponseField"];
export type TermValue = Field["value"];

const INPUT_TYPE: Record<NonNullable<Field["type"]>, string> = {
  STRING: "text",
  DATE: "date",
  TIME: "time",
  INTEGER: "text",
};

/** 입력란 글자를 서버로 보낼 값으로 바꾼다. 빈칸은 null, 정수 항목은 숫자(쉼표 허용) */
function toValue(raw: string, type: Field["type"]): TermValue {
  const text = raw.trim();
  if (text === "") return null;
  if (type === "INTEGER") {
    const digits = text.replaceAll(",", "");
    return /^\d+$/.test(digits) ? Number(digits) : text;
  }
  return text;
}

export function fieldInputId(key: string) {
  return `term-${key}`;
}

export interface TermFieldInputProps {
  field: Field;
  /** 근로자가 원본과 다르다고 신고한 항목 */
  reported?: boolean;
  /** 저장 실패 안내 (400 TERM_VALUE_INVALID, 제출 시 빈 필수 항목) */
  errorMessage?: string;
  /** 값이 바뀐 채로 입력란을 벗어나면 불린다 (R5 항목 수정) */
  onCommit: (key: string, value: TermValue) => void;
  /** 근거 조항 쪽으로 원본 미리보기를 넘긴다 */
  onShowSource: (pageNo: number) => void;
  labels: ContractDraftLabels;
}

/** 항목 하나. 서버 값이 바뀌면(저장 응답) 입력란을 새 값으로 다시 그린다. */
export function TermFieldInput({
  field,
  reported,
  errorMessage,
  onCommit,
  onShowSource,
  labels,
}: TermFieldInputProps) {
  const key = field.key ?? "";
  const id = fieldInputId(key);
  const serverValue = field.value ?? null;
  const unread =
    field.extractionStatus === "UNREAD" &&
    field.valueSource === "AI" &&
    serverValue === null;
  const sourceTag =
    field.valueSource === "EMPLOYER_EDIT" ||
    field.valueSource === "EMPLOYER_INPUT"
      ? labels.valueSources[field.valueSource]
      : undefined;
  const pageNo = field.sourceClause?.pageNo;
  const status = errorMessage
    ? "error"
    : unread || field.warning
      ? "warning"
      : undefined;
  const describedBy =
    [
      (errorMessage || unread) && `${id}-message`,
      field.warning && `${id}-warning`,
    ]
      .filter(Boolean)
      .join(" ") || undefined;

  function handleBlur(event: FocusEvent<HTMLInputElement>) {
    const next = toValue(event.target.value, field.type);
    if (String(next ?? "") === String(serverValue ?? "")) return;
    onCommit(key, next);
  }

  return (
    <div className="flex min-w-0 flex-[1_1_260px] flex-col gap-2">
      <div className="flex flex-wrap items-center gap-2">
        <label htmlFor={id} className="font-bold">
          {field.label}
          {field.required && (
            <>
              <span aria-hidden="true" className="ml-0.5 text-danger">
                *
              </span>
              <span className="sr-only">{labels.required}</span>
            </>
          )}
        </label>
        {sourceTag && <Badge tone="primary">{sourceTag}</Badge>}
        {reported && <Badge tone="warning">{labels.reported}</Badge>}
        {pageNo != null && (
          <button
            type="button"
            onClick={() => onShowSource(pageNo)}
            className="ml-auto cursor-pointer text-sm text-primary underline"
          >
            {labels.sourceClause(pageNo, field.sourceClause?.clauseNo)}
          </button>
        )}
      </div>
      <input
        key={`${key}:${String(serverValue ?? "")}`}
        id={id}
        name={key}
        type={INPUT_TYPE[field.type ?? "STRING"]}
        inputMode={field.type === "INTEGER" ? "numeric" : undefined}
        defaultValue={serverValue === null ? "" : String(serverValue)}
        onBlur={handleBlur}
        aria-invalid={errorMessage ? true : undefined}
        aria-describedby={describedBy}
        className={cn(fieldControlClass(status), "h-12")}
      />
      {(errorMessage || unread) && (
        <FieldMessage
          id={`${id}-message`}
          status={errorMessage ? "error" : "warning"}
        >
          {errorMessage ?? labels.unread}
        </FieldMessage>
      )}
      {field.warning && (
        <div
          id={`${id}-warning`}
          role="status"
          className="flex items-start gap-2.5 rounded-md border border-warning-border bg-warning-subtle px-4 py-3 text-sm"
        >
          <WarningIcon className="mt-0.5 shrink-0 text-warning" />
          <span>
            <strong>{labels.warningPrefix}</strong> {field.warning.message}
          </span>
        </div>
      )}
    </div>
  );
}
