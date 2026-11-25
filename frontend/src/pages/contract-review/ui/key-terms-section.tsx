import type { ReactNode } from "react";
import { formatTermValue } from "@/entities/contract";
import type { components } from "@/shared/api";
import { useCurrentLanguage } from "@/shared/i18n";
import { cn, countContractMonths, formatPeriod } from "@/shared/lib";
import { Highlight } from "@/shared/ui";
import type { ContractReviewLabels } from "../model/labels";

type KeyTerms = components["schemas"]["ContractReviewResponseKeyTerms"];

export interface KeyTermsSectionProps {
  keyTerms: KeyTerms;
  /** 고용주가 고치거나 입력한 항목 키 (형광펜) */
  employerKeys: Set<string>;
  /** 경고가 붙은 항목 키 (카드 강조) */
  warningKeys: Set<string>;
  labels: ContractReviewLabels;
}

interface Card {
  id: keyof ContractReviewLabels["keyTerms"];
  keys: string[];
  value: ReactNode;
  detail: ReactNode[];
}

/** '꼭 확인할 3가지' (임금·근로시간·계약기간) */
export function KeyTermsSection({
  keyTerms,
  employerKeys,
  warningKeys,
  labels,
}: KeyTermsSectionProps) {
  const language = useCurrentLanguage();
  function piece(key: string, text: ReactNode) {
    return employerKeys.has(key) ? <Highlight>{text}</Highlight> : text;
  }

  const months =
    keyTerms.startDate && keyTerms.endDate
      ? countContractMonths(keyTerms.startDate, keyTerms.endDate)
      : undefined;

  const cards: Card[] = [
    {
      id: "wage",
      keys: ["monthlyBaseWage", "payDay", "payMethod"],
      value: piece(
        "monthlyBaseWage",
        formatTermValue("monthlyBaseWage", keyTerms.monthlyBaseWage, labels),
      ),
      detail: [
        keyTerms.payDay != null &&
          piece("payDay", formatTermValue("payDay", keyTerms.payDay, labels)),
        keyTerms.payMethod && piece("payMethod", keyTerms.payMethod),
      ],
    },
    {
      id: "hours",
      keys: ["workStartTime", "workEndTime", "workDaysText", "breakMinutes"],
      value:
        keyTerms.workStartTime || keyTerms.workEndTime ? (
          <>
            {piece("workStartTime", keyTerms.workStartTime ?? "?")} –{" "}
            {piece("workEndTime", keyTerms.workEndTime ?? "?")}
          </>
        ) : (
          labels.noValue
        ),
      detail: [
        keyTerms.workDaysText && piece("workDaysText", keyTerms.workDaysText),
        keyTerms.weeklyHours !== undefined &&
          labels.weeklyHours(keyTerms.weeklyHours),
        keyTerms.breakMinutes != null && (
          <>
            {labels.breakTime}{" "}
            {piece(
              "breakMinutes",
              formatTermValue("breakMinutes", keyTerms.breakMinutes, labels),
            )}
          </>
        ),
      ],
    },
    {
      id: "period",
      keys: ["startDate", "endDate"],
      value: piece(
        "endDate",
        months === undefined
          ? keyTerms.startDate
            ? labels.noEndDate
            : labels.noValue
          : months >= 12
            ? labels.aboutYears(Math.round(months / 12))
            : labels.aboutMonths(months),
      ),
      detail: [
        keyTerms.startDate &&
          piece(
            "startDate",
            formatPeriod(keyTerms.startDate, keyTerms.endDate, language),
          ),
      ],
    },
  ];

  return (
    <section
      aria-labelledby="contract-review-key-terms"
      className="flex flex-col gap-4"
    >
      <h2 id="contract-review-key-terms" className="text-2xl font-bold">
        {labels.keyTermsTitle}
      </h2>
      <div className="flex flex-wrap gap-4">
        {cards.map((card) => {
          const warning = card.keys.some((key) => warningKeys.has(key));
          const detail = card.detail.filter(Boolean);
          return (
            <div
              key={card.id}
              className={cn(
                "flex flex-[1_1_200px] flex-col gap-1.5 rounded-lg p-5",
                warning
                  ? "border-2 border-warning-border bg-warning-surface"
                  : "border border-border",
              )}
            >
              <span className="text-sm font-bold text-muted">
                {labels.keyTerms[card.id]}
              </span>
              <strong className="text-3xl leading-[1.3] whitespace-nowrap">
                {card.value}
              </strong>
              {detail.length > 0 && (
                <span className="text-sm text-muted">
                  {detail.map((part, index) => (
                    <span key={index}>
                      {index > 0 && " · "}
                      {part}
                    </span>
                  ))}
                </span>
              )}
              {warning && (
                <span className="text-xs font-bold text-warning">
                  {labels.needsCheck}
                </span>
              )}
            </div>
          );
        })}
      </div>
    </section>
  );
}
