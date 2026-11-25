import type { UiLanguage } from "@/shared/i18n";

/** `2027-01-02` → ko `2027.01.02.`, vi `02/01/2027` */
export function formatDate(date: string, language: UiLanguage = "ko") {
  if (language === "vi") {
    const [year, month, day] = date.split("-");
    return `${day}/${month}/${year}`;
  }
  return `${date.replaceAll("-", ".")}.`;
}

/** 계약기간. 종료일이 없으면 시작일만 보인다. */
export function formatPeriod(
  startDate: string,
  endDate?: string | null,
  language: UiLanguage = "ko",
) {
  return endDate
    ? `${formatDate(startDate, language)} ~ ${formatDate(endDate, language)}`
    : `${formatDate(startDate, language)} ~`;
}

/** 계약 시작일부터 종료일까지의 길이를 반올림한 개월 수로 센다. */
export function countContractMonths(startDate: string, endDate: string) {
  const days = (Date.parse(endDate) - Date.parse(startDate)) / 86_400_000;
  return Math.round(days / (365.25 / 12));
}
