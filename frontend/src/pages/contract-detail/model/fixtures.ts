import type { components } from "@/shared/api";

type Detail = components["schemas"]["ContractDetailResponse"];

/**
 * 화면 확인용: 실제 M2 응답의 종료 상태만 바꿔 보인다. API 연동 단계에서 지운다.
 * ?termination=requested-by-me | requested-by-other | agreed
 */
export function withTerminationFixture(
  detail: Detail,
  state: string | null | undefined,
): Detail {
  const base = {
    terminationId: 1,
    reasonType: "MUTUAL",
    reasonText: "귀국 예정",
    endDate: "2027-06-30",
  } as const;
  const others = (detail.availableActions ?? []).filter(
    (action) =>
      action !== "REQUEST_TERMINATION" &&
      action !== "RESPOND_TERMINATION" &&
      action !== "CANCEL_TERMINATION",
  );

  switch (state) {
    case "requested-by-me":
      return {
        ...detail,
        termination: { ...base, status: "REQUESTED", requestedByMe: true },
        availableActions: [...others, "CANCEL_TERMINATION"],
      };
    case "requested-by-other":
      return {
        ...detail,
        termination: { ...base, status: "REQUESTED", requestedByMe: false },
        availableActions: [...others, "RESPOND_TERMINATION"],
      };
    case "agreed":
      return {
        ...detail,
        termination: { ...base, status: "AGREED", requestedByMe: false },
        availableActions: others,
      };
    default:
      return detail;
  }
}
