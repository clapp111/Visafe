import type { components } from "@/shared/api";

type Detail = components["schemas"]["ContractDetailResponse"];

/**
 * 화면 확인용 M2 응답 (와이어프레임 예시값). API 연동 단계에서 지운다.
 * ?state=recording 이면 블록체인 기록 중, 기본은 확정
 */
export function getAdminDoneFixture(
  contractId: number,
  state: string | null | undefined,
): Detail {
  const recording = state === "recording";
  const txHash =
    "0x4c1e7a90b3d2f5e8a1c6b9d0e4f7a2b5c8d1e3f6a9b0c2d5e7f8a1b4c6d9e9a7b";
  return {
    contractId,
    contractNo: "C-2026-04817",
    status: recording ? "IN_PROGRESS" : "CONFIRMED",
    stage: recording ? "RECORDING" : "DONE",
    confirmationNo: recording ? null : "VS-7K3F-9QXM",
    confirmedAt: recording ? null : "2026-11-13T06:14:00Z",
    employer: { companyName: "한빛정밀", representativeName: "김민수" },
    worker: { memberId: 2, name: "Nguyễn Văn An", nationality: "VN" },
    timeline: [
      { type: "SUBMITTED", occurredAt: "2026-11-13T04:24:00Z" },
      { type: "TRANSLATION_COMPLETED", occurredAt: "2026-11-13T04:30:00Z" },
      { type: "WORKER_UNDERSTOOD", occurredAt: "2026-11-13T05:05:00Z" },
      {
        type: "AUTHORITY_REVIEW_REQUESTED",
        occurredAt: "2026-11-13T05:05:01Z",
      },
      { type: "AUTHORITY_APPROVED", occurredAt: "2026-11-13T06:12:00Z" },
      { type: "CHAIN_RECORD_REQUESTED", occurredAt: "2026-11-13T06:12:01Z" },
      ...(recording
        ? []
        : ([
            { type: "CONFIRMED", occurredAt: "2026-11-13T06:14:00Z" },
          ] as const)),
    ],
    chainRecords: [
      {
        recordType: "CONFIRM",
        status: recording ? "SENT" : "MINED",
        network: "sepolia",
        txHash,
        minedAt: recording ? null : "2026-11-13T06:14:00Z",
        explorerUrl: `https://sepolia.etherscan.io/tx/${txHash}`,
      },
    ],
  };
}
