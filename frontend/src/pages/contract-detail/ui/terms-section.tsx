"use client";

import { useState } from "react";
import { FIELD_GROUPS, formatTermValue } from "@/entities/contract";
import type { components } from "@/shared/api";
import { cn } from "@/shared/lib";
import { ChevronDownIcon, Highlight } from "@/shared/ui";
import type { ContractDetailLabels } from "../model/labels";

type Term = components["schemas"]["ContractDetailResponseTerm"];

/** 처음에 보이는 항목 수. 나머지는 [항목 N개 더 보기]로 펼친다 */
const VISIBLE_COUNT = 10;
const ORDER = FIELD_GROUPS.flatMap((group) => group.rows.flat());
const LIST_ID = "contract-detail-terms";

/** 값이 있는 항목 (TermField 순서). 고용주가 고치거나 입력한 값은 형광펜 */
export function TermsSection({
  terms,
  labels,
}: {
  terms: Term[];
  labels: ContractDetailLabels;
}) {
  const [expanded, setExpanded] = useState(false);
  const ordered = [...terms].sort(
    (a, b) => ORDER.indexOf(a.key ?? "") - ORDER.indexOf(b.key ?? ""),
  );
  const hidden = Math.max(ordered.length - VISIBLE_COUNT, 0);
  const hasEmployerInput = ordered.some(
    (term) => term.valueSource && term.valueSource !== "AI",
  );

  return (
    <section
      aria-labelledby="contract-detail-terms-title"
      className="flex flex-col border-t-2 border-foreground pt-6"
    >
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2
          id="contract-detail-terms-title"
          className="text-2xl leading-normal font-bold"
        >
          {labels.termsTitle}
        </h2>
        {hasEmployerInput && (
          <span className="text-sm text-muted">
            <Highlight>{labels.highlightWord}</Highlight>{" "}
            {labels.highlightLegend}
          </span>
        )}
      </div>

      <ul id={LIST_ID}>
        {ordered.map((term, index) => {
          const byEmployer = term.valueSource && term.valueSource !== "AI";
          const value = formatTermValue(term.key, term.value, labels);
          return (
            <li
              key={term.key}
              hidden={!expanded && index >= VISIBLE_COUNT}
              className="flex flex-col gap-1 border-b border-divider py-4"
            >
              <span className="text-sm font-bold text-muted">{term.label}</span>
              <span className="text-lg font-bold">
                {byEmployer ? <Highlight>{value}</Highlight> : value}
              </span>
              {term.explanation && <span>{term.explanation}</span>}
            </li>
          );
        })}
      </ul>

      {hidden > 0 && (
        <button
          type="button"
          aria-expanded={expanded}
          aria-controls={LIST_ID}
          onClick={() => setExpanded((value) => !value)}
          className="mt-3 inline-flex min-h-11 cursor-pointer items-center gap-1 self-start text-primary"
        >
          {expanded ? labels.lessTerms : labels.moreTerms(hidden)}
          <ChevronDownIcon
            size={18}
            className={cn("transition-transform", expanded && "rotate-180")}
          />
        </button>
      )}
    </section>
  );
}
