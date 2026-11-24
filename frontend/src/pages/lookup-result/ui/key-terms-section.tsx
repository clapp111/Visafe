import type { components } from "@/shared/api";
import { countContractMonths, formatPeriod } from "@/shared/lib";
import { Table } from "@/shared/ui";
import type { LookupResultLabels } from "../model/labels";

type LookupResponse = components["schemas"]["LookupResponse"];

export interface KeyTermsSectionProps {
  keyTerms: NonNullable<LookupResponse["keyTerms"]>;
  startDate: string;
  endDate?: string | null;
  labels: LookupResultLabels;
}

/** 공개하는 근로시간과 계약기간 (임금 제외) */
export function KeyTermsSection({
  keyTerms,
  startDate,
  endDate,
  labels,
}: KeyTermsSectionProps) {
  const workDays =
    keyTerms.workDaysText ??
    (keyTerms.workDaysPerWeek !== undefined &&
      labels.workDays(keyTerms.workDaysPerWeek));
  const months = endDate ? countContractMonths(startDate, endDate) : undefined;
  const length =
    months === undefined
      ? labels.noEndDate
      : months >= 12
        ? labels.aboutYears(Math.round(months / 12))
        : labels.aboutMonths(months);

  return (
    <>
      <p className="text-sm text-muted">{labels.keyTermsHint}</p>
      <Table caption={labels.keyTermsTitle}>
        <tbody>
          <tr>
            <th scope="row" className="w-1/4">
              {labels.workHours}
            </th>
            <td className="font-bold">
              {keyTerms.workStartTime} – {keyTerms.workEndTime}
              {workDays && ` (${workDays})`}
            </td>
            <td className="text-sm text-muted">
              {keyTerms.weeklyHours !== undefined &&
                keyTerms.breakMinutes !== undefined &&
                labels.weeklySummary(
                  keyTerms.weeklyHours,
                  keyTerms.breakMinutes,
                )}
            </td>
          </tr>
          <tr>
            <th scope="row" className="w-1/4">
              {labels.period}
            </th>
            <td className="font-bold">{formatPeriod(startDate, endDate)}</td>
            <td className="text-sm text-muted">{length}</td>
          </tr>
        </tbody>
      </Table>
    </>
  );
}
