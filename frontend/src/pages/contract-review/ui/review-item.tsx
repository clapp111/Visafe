"use client";

import { useState } from "react";
import { formatTermValue, termLabel } from "@/entities/contract";
import type { components } from "@/shared/api";
import { useCurrentLanguage } from "@/shared/i18n";
import { cn } from "@/shared/lib";
import { Badge, ChevronDownIcon, Highlight, WarningIcon } from "@/shared/ui";
import type { ContractReviewLabels } from "../model/labels";

type Item = components["schemas"]["ContractReviewResponseItem"];
type Warning = components["schemas"]["ContractReviewResponseWarning"];

export interface ReviewItemProps {
  item: Item;
  /** 이 항목에 붙은 경고 */
  warnings: Warning[];
  /** 번역 언어 (번역 값·쉬운 설명의 lang) */
  language?: string;
  /** 근거 조항 쪽으로 원본 미리보기를 넘긴다 */
  onShowSource: (pageNo: number) => void;
  labels: ContractReviewLabels;
}

/** 항목 하나: 원본 값, 번역 값, 쉬운 설명. 고용주 입력이면 형광펜과 AI 값 비교 */
export function ReviewItem({
  item,
  warnings,
  language,
  onShowSource,
  labels,
}: ReviewItemProps) {
  const uiLanguage = useCurrentLanguage();
  const [open, setOpen] = useState(false);
  const key = item.key ?? "";
  const panelId = `contract-review-input-${key}`;
  const byEmployer =
    item.valueSource !== undefined && item.valueSource !== "AI";
  const value = formatTermValue(key, item.value, labels, uiLanguage);
  const pageNo = item.sourceClause?.pageNo;
  const checkMessages = warnings.filter(
    (warning) => warning.code === "NEEDS_CHECK" && warning.message,
  );
  const aiValue =
    item.employerInput?.extractionStatus === "UNREAD" ||
    item.employerInput?.aiValue == null
      ? labels.aiUnread
      : formatTermValue(key, item.employerInput.aiValue, labels, uiLanguage);

  return (
    <div
      className={cn(
        "flex flex-col gap-1.5 border-b border-divider py-5",
        byEmployer && "-mx-4 bg-highlight-subtle/40 px-4",
      )}
    >
      <div className="flex flex-wrap items-center gap-2">
        <span className="text-sm font-bold text-muted">
          {termLabel(key, item.label, uiLanguage)}
        </span>
        {byEmployer && <Badge tone="highlight">{labels.employerInput}</Badge>}
        {warnings.length > 0 && (
          <Badge tone="warning">{labels.needsCheck}</Badge>
        )}
        {pageNo != null && (
          <button
            type="button"
            onClick={() => onShowSource(pageNo)}
            className="ml-auto cursor-pointer text-sm text-primary underline"
          >
            {labels.showSource(pageNo)}
          </button>
        )}
      </div>

      <span className="text-lg font-bold">
        {byEmployer ? <Highlight>{value}</Highlight> : value}
      </span>
      {item.translatedValue && (
        <p lang={language} className="text-sm text-muted">
          {item.translatedValue}
        </p>
      )}
      {item.easyExplanation && <p lang={language}>{item.easyExplanation}</p>}

      {checkMessages.map((warning) => (
        <p
          key={warning.code}
          className="flex items-start gap-2 text-sm text-warning"
        >
          <WarningIcon size={18} className="mt-0.5 shrink-0" />
          <span lang={language}>{warning.message}</span>
        </p>
      ))}

      {byEmployer && (
        <>
          <button
            type="button"
            aria-expanded={open}
            aria-controls={panelId}
            onClick={() => setOpen((value) => !value)}
            className="inline-flex min-h-9 cursor-pointer items-center gap-1 self-start text-sm text-primary"
          >
            {open ? labels.employerInputClose : labels.employerInputOpen}
            <ChevronDownIcon
              size={16}
              className={cn("transition-transform", open && "rotate-180")}
            />
          </button>
          <div
            id={panelId}
            hidden={!open}
            className="mt-1 flex-col gap-2 rounded-md border border-highlight-border bg-surface p-4 text-sm [&:not([hidden])]:flex"
          >
            <span>
              <strong>{labels.aiValue}</strong> {aiValue}
            </span>
            <span>
              <strong>{labels.employerValue}</strong> {value}
            </span>
          </div>
        </>
      )}
    </div>
  );
}
