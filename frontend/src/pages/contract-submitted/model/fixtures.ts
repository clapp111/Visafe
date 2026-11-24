import type { components } from "@/shared/api";

type Detail = components["schemas"]["ContractDetailResponse"];

// 화면 확인용 와이어프레임 예시값. M2 진행 상황 조회를 연결하면 지운다.
const TERMS: Detail["terms"] = Array.from({ length: 19 }, (_, index) => ({
  key: `term${index}`,
  label: `항목 ${index + 1}`,
  // 와이어프레임처럼 값이 있는 항목 18개 (선택 항목 하나는 비어 있음)
  value: index === 18 ? null : `값 ${index + 1}`,
  valueSource: "AI",
}));

const TRANSLATING: Detail = {
  contractId: 1,
  contractNo: "C-2026-04817",
  status: "IN_PROGRESS",
  stage: "TRANSLATING",
  processing: { task: "TRANSLATION", status: "RUNNING", attempt: 1 },
  worker: { memberId: 12, name: "Nguyễn Văn An", nationality: "VN" },
  terms: TERMS,
  timeline: [
    {
      type: "SUBMITTED",
      occurredAt: "2026-11-13T04:24:00Z",
      actorLabel: "한빛정밀",
    },
  ],
};

const FAILED: Detail = {
  ...TRANSLATING,
  processing: {
    task: "TRANSLATION",
    status: "FAILED",
    attempt: 3,
    lastError: "LLM 응답 시간 초과",
  },
  availableActions: ["RETRY_TRANSLATION"],
};

const AWAITING: Detail = {
  ...TRANSLATING,
  stage: "AWAITING_WORKER",
  processing: undefined,
  timeline: [
    ...(TRANSLATING.timeline ?? []),
    { type: "TRANSLATION_COMPLETED", occurredAt: "2026-11-13T04:27:00Z" },
  ],
};

/** 주소의 계약 ID로 예시 상태를 고른다. 1 번역 중, 2 번역 실패, 3 근로자 확인 대기 */
export function getSubmittedFixture(contractId: number): Detail {
  return [TRANSLATING, FAILED, AWAITING][contractId - 1] ?? TRANSLATING;
}
