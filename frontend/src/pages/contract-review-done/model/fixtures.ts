import type { components } from "@/shared/api";

type Detail = components["schemas"]["ContractDetailResponse"];
type Timeline = NonNullable<Detail["timeline"]>;

/** 화면 확인용 M2 응답 (와이어프레임 예시값). API 연동 단계에서 지운다. */
const TERM_KEYS = [
  "employerName",
  "representativeName",
  "employerAddress",
  "workerName",
  "workerNationality",
  "startDate",
  "endDate",
  "workPlace",
  "jobDescription",
  "workStartTime",
  "workEndTime",
  "breakMinutes",
  "workDaysPerWeek",
  "workDaysText",
  "weeklyHoliday",
  "monthlyBaseWage",
  "payDay",
  "payMethod",
];

const BASE_TIMELINE: Timeline = [
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
  {
    type: "WORKER_UNDERSTOOD",
    occurredAt: "2026-11-13T05:05:00Z",
    actorLabel: "Nguyễn Văn An",
  },
];

/**
 * ?state=recording(기본) | failed(기록 실패) | authority(기관 검토) | done(확정)
 */
export function getReviewDoneFixture(
  contractId: number,
  state?: string | null,
): Detail {
  const base: Detail = {
    contractId,
    contractNo: "C-2026-04817",
    status: "IN_PROGRESS",
    stage: "RECORDING",
    processing: { task: "CHAIN_RECORD", status: "RUNNING", attempt: 1 },
    confirmationNo: null,
    confirmedAt: null,
    worker: { memberId: 2, name: "Nguyễn Văn An", nationality: "VN" },
    terms: TERM_KEYS.map((key) => ({ key, value: "예시" })),
    timeline: [
      ...BASE_TIMELINE,
      { type: "CHAIN_RECORD_REQUESTED", occurredAt: "2026-11-13T05:05:01Z" },
    ],
    chainRecords: [],
  };

  if (state === "failed") {
    return {
      ...base,
      processing: {
        task: "CHAIN_RECORD",
        status: "FAILED",
        attempt: 1,
        lastError: "RPC timeout",
      },
    } satisfies Detail;
  }
  if (state === "authority") {
    return {
      ...base,
      stage: "AUTHORITY_REVIEW",
      processing: undefined,
      timeline: [
        ...BASE_TIMELINE,
        {
          type: "AUTHORITY_REVIEW_REQUESTED",
          occurredAt: "2026-11-13T05:05:01Z",
        },
      ],
    } satisfies Detail;
  }
  if (state === "done") {
    return {
      ...base,
      status: "CONFIRMED",
      stage: "DONE",
      processing: undefined,
      confirmationNo: "VS-7K3F-9QXM",
      confirmedAt: "2026-11-13T05:06:00Z",
      timeline: [
        ...BASE_TIMELINE,
        { type: "CHAIN_RECORD_REQUESTED", occurredAt: "2026-11-13T05:05:01Z" },
        { type: "CONFIRMED", occurredAt: "2026-11-13T05:06:00Z" },
      ],
      chainRecords: [
        {
          recordType: "CONFIRM",
          status: "MINED",
          network: "sepolia",
          txHash:
            "0x8f3a5c1e9b2d4f60718293a4b5c6d7e8f9012345678901234567890abcdef12",
          minedAt: "2026-11-13T05:06:00Z",
          explorerUrl:
            "https://sepolia.etherscan.io/tx/0x8f3a5c1e9b2d4f60718293a4b5c6d7e8f9012345678901234567890abcdef12",
        },
      ],
    } satisfies Detail;
  }
  return base;
}
