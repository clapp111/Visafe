import type { components } from "@/shared/api";
import type { ContractRole } from "./labels";

type Detail = components["schemas"]["ContractDetailResponse"];
type Term = components["schemas"]["ContractDetailResponseTerm"];
type Timeline = NonNullable<Detail["timeline"]>;

/** 화면 확인용 M2 응답 (와이어프레임 예시값). API 연동 단계에서 지운다. */
const TERMS: Term[] = [
  {
    key: "employerName",
    label: "사업체명",
    value: "한빛정밀",
    valueSource: "AI",
    explanation: "나를 고용하는 회사입니다.",
  },
  {
    key: "representativeName",
    label: "대표자",
    value: "김민수",
    valueSource: "AI",
    explanation: "회사를 대표해 계약한 사람입니다.",
  },
  {
    key: "employerAddress",
    label: "사업장 주소",
    value: "경기도 안산시 단원구 ○○로 12",
    valueSource: "AI",
    explanation: "회사가 있는 곳의 주소입니다.",
  },
  {
    key: "workerName",
    label: "근로자 성명",
    value: "Nguyễn Văn An",
    valueSource: "AI",
    explanation: "이 계약을 맺은 근로자입니다.",
  },
  {
    key: "workerNationality",
    label: "국적",
    value: "베트남",
    valueSource: "AI",
    explanation: "계약서에 적힌 근로자의 국적입니다.",
  },
  {
    key: "startDate",
    label: "계약 시작일",
    value: "2027-01-02",
    valueSource: "AI",
    explanation: "2027년 1월 2일부터 일합니다.",
  },
  {
    key: "endDate",
    label: "계약 종료일",
    value: "2029-12-31",
    valueSource: "AI",
    explanation: "약 3년 동안 일합니다.",
  },
  {
    key: "workPlace",
    label: "근무 장소",
    value: "사업장 주소와 같음",
    valueSource: "AI",
    explanation: "경기도 안산시 단원구 ○○로 12에서 일합니다.",
  },
  {
    key: "jobDescription",
    label: "업무 내용",
    value: "금속 부품 가공 및 포장",
    valueSource: "EMPLOYER_EDIT",
    explanation: "금속 부품을 가공하고 포장합니다.",
  },
  {
    key: "workStartTime",
    label: "시업 시각",
    value: "09:00",
    valueSource: "AI",
    explanation: "아침 9시에 일을 시작합니다.",
  },
  {
    key: "workEndTime",
    label: "종업 시각",
    value: "18:00",
    valueSource: "AI",
    explanation: "저녁 6시에 일이 끝납니다.",
  },
  {
    key: "breakMinutes",
    label: "휴게시간(분)",
    value: 60,
    valueSource: "EMPLOYER_INPUT",
    explanation: "일하는 중간에 1시간 쉽니다.",
  },
  {
    key: "workDaysPerWeek",
    label: "주 근무일 수",
    value: 5,
    valueSource: "AI",
    explanation: "일주일에 5일 일합니다.",
  },
  {
    key: "workDaysText",
    label: "근무일 원문 표현",
    value: "월~금",
    valueSource: "AI",
    explanation: "월요일부터 금요일까지 일합니다.",
  },
  {
    key: "weeklyHoliday",
    label: "주휴일",
    value: "매주 일요일",
    valueSource: "AI",
    explanation: "일요일은 쉬는 날입니다.",
  },
  {
    key: "monthlyBaseWage",
    label: "월 기본급(원)",
    value: 2000000,
    valueSource: "AI",
    explanation: "한 달에 기본급으로 200만 원을 받습니다.",
  },
  {
    key: "payDay",
    label: "임금 지급일",
    value: 10,
    valueSource: "AI",
    explanation: "매달 10일에 임금을 받습니다.",
  },
  {
    key: "payMethod",
    label: "지급 방법",
    value: "본인 명의 계좌 입금",
    valueSource: "AI",
    explanation: "내 계좌로 들어옵니다.",
  },
  {
    key: "accommodation",
    label: "숙식 제공",
    value: "숙소 제공 · 근로자 부담 없음",
    valueSource: "AI",
    explanation: "숙소 비용을 월급에서 빼지 않습니다.",
  },
];

const SUBMITTED: Timeline = [
  {
    type: "CONTRACT_CREATED",
    occurredAt: "2026-11-13T04:10:00Z",
    actorLabel: "한빛정밀",
  },
  {
    type: "TERM_EDITED",
    occurredAt: "2026-11-13T04:20:00Z",
    actorLabel: "한빛정밀",
    detail: { keys: ["jobDescription", "breakMinutes"] },
  },
  {
    type: "SUBMITTED",
    occurredAt: "2026-11-13T04:24:00Z",
    actorLabel: "한빛정밀",
  },
  {
    type: "TRANSLATION_COMPLETED",
    occurredAt: "2026-11-13T04:30:00Z",
    actorLabel: null,
  },
];
const UNDERSTOOD: Timeline = [
  ...SUBMITTED,
  {
    type: "WORKER_UNDERSTOOD",
    occurredAt: "2026-11-13T05:05:00Z",
    actorLabel: "Nguyễn Văn An",
    detail: { language: "vi" },
  },
  {
    type: "CHAIN_RECORD_REQUESTED",
    occurredAt: "2026-11-13T05:05:01Z",
    actorLabel: null,
  },
];
const CONFIRMED: Timeline = [
  ...UNDERSTOOD,
  { type: "CONFIRMED", occurredAt: "2026-11-13T05:06:00Z", actorLabel: null },
];
const CONFIRM_RECORD = {
  recordType: "CONFIRM",
  status: "MINED",
  network: "sepolia",
  txHash: "0x4c1e7a90b2d3f4e5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b56d9e9a7b",
  minedAt: "2026-11-13T05:06:00Z",
  explorerUrl:
    "https://sepolia.etherscan.io/tx/0x4c1e7a90b2d3f4e5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b56d9e9a7b",
} as const;

/**
 * ?state=done(기본) | reviewing | awaiting | returned | recording | ended
 * 할 수 있는 일(availableActions)은 로그인한 역할에 맞춘다.
 */
export function getDetailFixture(
  contractId: number,
  state: string | null | undefined,
  role: ContractRole,
): Detail {
  const employer = role === "EMPLOYER";
  const base: Detail = {
    contractId,
    contractNo: "C-2026-05120",
    status: "IN_PROGRESS",
    employer: { companyName: "한빛정밀", representativeName: "김민수" },
    worker: { memberId: 2, name: "Nguyễn Văn An", nationality: "VN" },
    // 시드의 worker1 계약 4 원본 (원본 미리보기는 실제 M3를 부른다)
    file: {
      fileId: 6,
      fileName: "contract_sample.pdf",
      mimeType: "application/pdf",
      sizeBytes: 1_150_000,
      pageCount: 2,
    },
    terms: TERMS,
    chainRecords: [],
  };

  switch (state) {
    case "reviewing":
      return {
        ...base,
        stage: "REVIEWING",
        timeline: SUBMITTED.slice(0, 2),
        availableActions: employer
          ? ["EDIT_DRAFT", "DOWNLOAD_FILE"]
          : ["DOWNLOAD_FILE"],
      };
    case "awaiting":
      return {
        ...base,
        stage: "AWAITING_WORKER",
        timeline: SUBMITTED,
        availableActions: employer
          ? ["DOWNLOAD_FILE"]
          : ["RESPOND_REVIEW", "DOWNLOAD_FILE"],
      };
    case "returned":
      return {
        ...base,
        stage: "RETURNED",
        timeline: [
          ...SUBMITTED,
          {
            type: "WORKER_REPORTED",
            occurredAt: "2026-11-13T05:05:00Z",
            actorLabel: "Nguyễn Văn An",
            detail: {
              keys: ["breakMinutes"],
              reasonKo: "원본에는 휴게시간이 30분으로 적혀 있어요.",
            },
          },
        ],
        availableActions: employer
          ? ["START_REVISION", "DOWNLOAD_FILE"]
          : ["DOWNLOAD_FILE"],
      };
    case "recording":
      return {
        ...base,
        stage: "RECORDING",
        timeline: UNDERSTOOD,
        chainRecords: [{ ...CONFIRM_RECORD, status: "SENT", minedAt: null }],
        availableActions: ["DOWNLOAD_FILE"],
      };
    case "ended":
      return {
        ...base,
        status: "TERMINATED",
        stage: "ENDED",
        confirmationNo: "VS-7K3F-9QXM",
        confirmedAt: "2026-11-13T05:06:00Z",
        terminatedAt: "2027-06-30T09:00:00Z",
        timeline: [
          ...CONFIRMED,
          {
            type: "TERMINATION_REQUESTED",
            occurredAt: "2027-06-01T01:00:00Z",
            actorLabel: "Nguyễn Văn An",
          },
          {
            type: "TERMINATION_AGREED",
            occurredAt: "2027-06-02T01:00:00Z",
            actorLabel: "한빛정밀",
          },
          {
            type: "TERMINATED",
            occurredAt: "2027-06-30T09:00:00Z",
            actorLabel: null,
          },
        ],
        chainRecords: [
          CONFIRM_RECORD,
          {
            ...CONFIRM_RECORD,
            recordType: "TERMINATE",
            minedAt: "2027-06-30T09:00:00Z",
            txHash:
              "0x9a7b4c1e7a90b2d3f4e5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b56d9e",
          },
        ],
        availableActions: ["DOWNLOAD_FILE"],
      };
    default:
      return {
        ...base,
        status: "CONFIRMED",
        stage: "DONE",
        confirmationNo: "VS-7K3F-9QXM",
        confirmedAt: "2026-11-13T05:06:00Z",
        timeline: CONFIRMED,
        chainRecords: [CONFIRM_RECORD],
        availableActions: ["REQUEST_TERMINATION", "DOWNLOAD_FILE"],
      };
  }
}
