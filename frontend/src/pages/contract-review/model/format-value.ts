import { formatDate } from "@/shared/lib";
import type { ContractReviewLabels } from "./labels";

/**
 * 항목 값을 화면 글자로 바꾼다. W1 value는 날짜 2027-01-02, 시각 09:00, 정수,
 * employerInput.aiValue는 같은 값을 글자로 준다.
 */
export function formatTermValue(
  key: string | undefined,
  value: string | number | null | undefined,
  labels: ContractReviewLabels,
) {
  if (value === null || value === undefined || value === "") {
    return labels.noValue;
  }
  const number = typeof value === "number" ? value : Number(value);
  const isNumber = Number.isFinite(number) && String(value).trim() !== "";

  switch (key) {
    case "startDate":
    case "endDate":
      return formatDate(String(value));
    case "monthlyBaseWage":
      return isNumber
        ? labels.units.won(number.toLocaleString("ko-KR"))
        : String(value);
    case "breakMinutes":
      return isNumber ? labels.units.minutes(number) : String(value);
    case "workDaysPerWeek":
      return isNumber ? labels.units.daysPerWeek(number) : String(value);
    case "payDay":
      return isNumber ? labels.units.payDay(number) : String(value);
    default:
      return String(value);
  }
}
