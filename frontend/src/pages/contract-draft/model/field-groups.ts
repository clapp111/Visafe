import type { CONTRACT_DRAFT_LABELS } from "./labels";

type GroupId = keyof typeof CONTRACT_DRAFT_LABELS.groups;

/**
 * 19개 항목을 와이어프레임의 네 묶음과 줄로 나눈다. 한 줄에 둘이면 넓은 화면에서 나란히 놓인다.
 * 키는 R4 fields[].key (TermField 순서와 같다)
 */
export const FIELD_GROUPS: { id: GroupId; rows: string[][] }[] = [
  {
    id: "parties",
    rows: [
      ["employerName", "representativeName"],
      ["employerAddress"],
      ["workerName", "workerNationality"],
    ],
  },
  {
    id: "period",
    rows: [
      ["startDate", "endDate"],
      ["workPlace", "jobDescription"],
    ],
  },
  {
    id: "hours",
    rows: [
      ["workStartTime", "workEndTime"],
      ["breakMinutes", "workDaysPerWeek"],
      ["workDaysText", "weeklyHoliday"],
    ],
  },
  {
    id: "wage",
    rows: [["monthlyBaseWage"], ["payDay", "payMethod"], ["accommodation"]],
  },
];
