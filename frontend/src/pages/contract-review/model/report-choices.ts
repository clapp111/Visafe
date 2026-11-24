import type { ContractReviewLabels } from "./labels";

type ChoiceId = keyof ContractReviewLabels["reportChoices"];

/**
 * '원본과 다릅니다' 선택지. 화면에 한 줄로 보이는 항목은 키 여러 개로 함께 보낸다.
 * 키는 W1 items[].key · W2 reportItems[].key (TermField)
 */
export const REPORT_CHOICES: { id: ChoiceId; keys: string[] }[] = [
  { id: "employer", keys: ["employerName", "representativeName"] },
  { id: "employerAddress", keys: ["employerAddress"] },
  { id: "worker", keys: ["workerName", "workerNationality"] },
  { id: "period", keys: ["startDate", "endDate"] },
  { id: "workPlace", keys: ["workPlace"] },
  { id: "jobDescription", keys: ["jobDescription"] },
  {
    id: "workHours",
    keys: ["workStartTime", "workEndTime", "workDaysPerWeek", "workDaysText"],
  },
  { id: "breakMinutes", keys: ["breakMinutes"] },
  { id: "weeklyHoliday", keys: ["weeklyHoliday"] },
  { id: "monthlyBaseWage", keys: ["monthlyBaseWage"] },
  { id: "payment", keys: ["payDay", "payMethod"] },
  { id: "accommodation", keys: ["accommodation"] },
];
