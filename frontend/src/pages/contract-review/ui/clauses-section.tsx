"use client";

import { useState } from "react";
import type { components } from "@/shared/api";
import { cn } from "@/shared/lib";
import { ChevronDownIcon } from "@/shared/ui";
import type { ContractReviewLabels } from "../model/labels";

type Clause = components["schemas"]["ContractReviewResponseClause"];

const LIST_ID = "contract-review-clauses";

export interface ClausesSectionProps {
  clauses: Clause[];
  language?: string;
  onShowSource: (pageNo: number) => void;
  labels: ContractReviewLabels;
}

/** 원문·번역 조항 (읽는 순서). 길어서 처음에는 접어 둔다. */
export function ClausesSection({
  clauses,
  language,
  onShowSource,
  labels,
}: ClausesSectionProps) {
  const [open, setOpen] = useState(false);

  return (
    <section
      aria-labelledby="contract-review-clauses-title"
      className="flex flex-col gap-4 border-t-2 border-foreground pt-6"
    >
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2
          id="contract-review-clauses-title"
          className="text-2xl leading-normal font-bold"
        >
          {labels.clausesTitle(clauses.length)}
        </h2>
        <button
          type="button"
          aria-expanded={open}
          aria-controls={LIST_ID}
          onClick={() => setOpen((value) => !value)}
          className="inline-flex min-h-11 cursor-pointer items-center gap-1 text-primary"
        >
          {open ? labels.clausesClose : labels.clausesOpen}
          <ChevronDownIcon
            size={18}
            className={cn("transition-transform", open && "rotate-180")}
          />
        </button>
      </div>
      <p className="text-sm text-muted">{labels.clausesHint}</p>

      <ol
        id={LIST_ID}
        hidden={!open}
        className="flex-col gap-6 [&:not([hidden])]:flex"
      >
        {clauses.map((clause) => (
          <li
            key={clause.clauseId ?? clause.seq}
            className="flex flex-col gap-3"
          >
            <div className="flex flex-wrap items-center gap-2">
              <strong>
                {labels.clauseTitle(clause.seq ?? 0, clause.clauseNo)}
              </strong>
              {clause.pageNo != null && (
                <button
                  type="button"
                  onClick={() => onShowSource(clause.pageNo ?? 1)}
                  className="ml-auto cursor-pointer text-sm text-primary underline"
                >
                  {labels.showSource(clause.pageNo)}
                </button>
              )}
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
              <div className="flex flex-col gap-1 rounded-md bg-surface-muted p-4">
                <span className="text-xs font-bold text-muted">
                  {labels.original}
                </span>
                <p lang="ko" className="text-sm">
                  {clause.textKo}
                </p>
              </div>
              <div className="flex flex-col gap-1 rounded-md border border-divider p-4">
                <span className="text-xs font-bold text-muted">
                  {labels.translation}
                </span>
                {clause.translatedText ? (
                  <p lang={language} className="text-sm">
                    {clause.translatedText}
                  </p>
                ) : (
                  <p className="text-sm text-muted">{labels.noTranslation}</p>
                )}
              </div>
            </div>
          </li>
        ))}
      </ol>
    </section>
  );
}
