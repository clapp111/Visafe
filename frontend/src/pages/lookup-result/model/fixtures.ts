import type { components } from "@/shared/api";

type LookupResponse = components["schemas"]["LookupResponse"];

// 화면 확인용 와이어프레임 예시값. L1 조회를 연결하면 지운다.
const CONFIRMED: LookupResponse = {
  confirmationNo: "VS-7K3F-9QXM",
  status: "CONFIRMED",
  companyName: "한빛정밀",
  workerNameMasked: "N***** V** An",
  startDate: "2027-01-02",
  endDate: "2029-12-31",
  confirmedAt: "2026-11-13T05:06:00Z",
  keyTerms: {
    workStartTime: "09:00",
    workEndTime: "18:00",
    breakMinutes: 60,
    workDaysPerWeek: 5,
    workDaysText: "월~금",
    weeklyHours: 40,
  },
  integrity: { status: "VERIFIED", checkedAt: "2026-11-18T10:12:00Z" },
  termination: undefined,
  chainRecords: [
    {
      recordType: "CONFIRM",
      status: "MINED",
      network: "sepolia",
      txHash:
        "0x4c1e7a9015b3d2e8f0a6c4b9e7d1f3a5c8b2e4d6f0a1c3e5b7d9f2a46d9e9a7b",
      minedAt: "2026-11-13T05:06:00Z",
      explorerUrl:
        "https://sepolia.etherscan.io/tx/0x4c1e7a9015b3d2e8f0a6c4b9e7d1f3a5c8b2e4d6f0a1c3e5b7d9f2a46d9e9a7b",
    },
  ],
  history: [
    {
      type: "SUBMITTED",
      occurredAt: "2026-11-13T04:24:00Z",
      actorLabel: "한빛정밀",
    },
    {
      type: "WORKER_UNDERSTOOD",
      occurredAt: "2026-11-13T05:05:00Z",
      actorLabel: null,
      detail: { language: "vi" },
    },
    { type: "CONFIRMED", occurredAt: "2026-11-13T05:06:00Z", actorLabel: null },
  ],
};

const TERMINATED: LookupResponse = {
  confirmationNo: "VS-3HPD-8WNA",
  status: "TERMINATED",
  companyName: "서해수산",
  workerNameMasked: "L* V** H***",
  startDate: "2025-03-02",
  endDate: "2027-03-01",
  confirmedAt: "2025-02-20T01:12:00Z",
  keyTerms: {
    workStartTime: "08:00",
    workEndTime: "17:00",
    breakMinutes: 60,
    workDaysPerWeek: 6,
    workDaysText: "월~토",
    weeklyHours: 48,
  },
  integrity: { status: "VERIFIED", checkedAt: "2026-11-18T10:12:00Z" },
  termination: {
    reasonType: "MUTUAL",
    endDate: "2026-06-30",
    terminatedAt: "2026-06-22T07:40:00Z",
  },
  chainRecords: [
    {
      recordType: "CONFIRM",
      status: "MINED",
      network: "sepolia",
      txHash:
        "0x9a2b4c6d8e0f1a3b5c7d9e1f2a4b6c8d0e2f4a6b8c0d2e4f6a8b0c2d1e3f5a7b",
      minedAt: "2025-02-20T01:12:00Z",
      explorerUrl:
        "https://sepolia.etherscan.io/tx/0x9a2b4c6d8e0f1a3b5c7d9e1f2a4b6c8d0e2f4a6b8c0d2e4f6a8b0c2d1e3f5a7b",
    },
    {
      recordType: "TERMINATE",
      status: "MINED",
      network: "sepolia",
      txHash:
        "0x7f1e3d5c9b8a7f6e5d4c3b2a1f0e9d8c7b6a5f4e3d2c1b0a9f8e7d62b4a6c8e",
      minedAt: "2026-06-22T07:40:00Z",
      explorerUrl:
        "https://sepolia.etherscan.io/tx/0x7f1e3d5c9b8a7f6e5d4c3b2a1f0e9d8c7b6a5f4e3d2c1b0a9f8e7d62b4a6c8e",
    },
  ],
  history: [
    { type: "CONFIRMED", occurredAt: "2025-02-20T01:12:00Z", actorLabel: null },
    {
      type: "TERMINATED",
      occurredAt: "2026-06-22T07:40:00Z",
      actorLabel: null,
    },
  ],
};

const FIXTURES = [CONFIRMED, TERMINATED];

/**
 * 확인번호에 맞는 예시 결과를 고른다. 예시에 없는 번호는 404, 형식이 틀린 번호는 400처럼 다룬다.
 */
export function getLookupFixture(
  confirmationNo: string,
):
  | { result: LookupResponse }
  | { errorCode: "LOOKUP_NOT_FOUND" | "LOOKUP_INVALID_FORMAT" } {
  const result = FIXTURES.find(
    (fixture) => fixture.confirmationNo === confirmationNo,
  );
  if (result) {
    return { result };
  }
  return /^VS-[2-9A-HJ-NP-Z]{4}-[2-9A-HJ-NP-Z]{4}$/.test(confirmationNo)
    ? { errorCode: "LOOKUP_NOT_FOUND" }
    : { errorCode: "LOOKUP_INVALID_FORMAT" };
}
