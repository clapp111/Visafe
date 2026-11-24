import type { components } from "@/shared/api";

type Detail = components["schemas"]["ContractDetailResponse"];

export type ReviewDonePhase = "recording" | "authority" | "done";

/** 근로자 확인 완료 문구. 화면 언어(#77)가 붙으면 언어별 값으로 바꾼다. */
export const CONTRACT_REVIEW_DONE_LABELS = {
  breadcrumb: "현재 위치",
  home: "홈",
  contracts: "내 계약",
  current: "계약 내용 확인",
  loading: "확인 진행 상황을 불러오는 중",
  loadError: "확인 진행 상황을 불러오지 못했습니다",

  title: {
    recording: "확인이 기록되었습니다",
    authority: "확인이 기록되었습니다",
    done: "계약이 확정되었습니다",
  } satisfies Record<ReviewDonePhase, string>,
  description: {
    recording:
      "지금 보신 계약 내용과 번역으로 확인이 기록되었습니다. 블록체인 기록이 끝나면 계약이 확정되고 확인번호를 알려 드립니다.",
    authority:
      "지금 보신 계약 내용과 번역으로 확인이 기록되었습니다. 이 계약은 감독 기관이 확인한 뒤 블록체인에 기록되고 확인번호가 발급됩니다.",
    done: "계약 내용이 블록체인에 기록되었습니다. 아래 확인번호로 누구나 이 계약이 유효한지 조회할 수 있습니다.",
  } satisfies Record<ReviewDonePhase, string>,
  contractNo: "계약 번호",
  currentStatus: "현재 상태",
  statuses: {
    IN_PROGRESS: "진행",
    CONFIRMED: "확정",
    TERMINATED: "종료",
  } satisfies Record<NonNullable<Detail["status"]>, string>,
  confirmationNo: "확인번호",
  copy: "복사",
  copied: "복사했습니다",
  confirmedAt: "확정 일시",
  chainRecord: "블록체인 기록",
  explorer: "탐색기에서 보기",

  progressTitle: "진행 상황",
  steps: {
    submitted: {
      title: "고용주 제출",
      description: (count: number) =>
        `원본 파일과 검토한 항목 ${count}개가 저장되었습니다.`,
    },
    translation: {
      title: "번역",
      description: "번역이 끝나 확인 요청이 전달되었습니다.",
    },
    worker: {
      title: "근로자 확인",
      description: (name?: string | null) =>
        name ? `${name} 님이 확인했습니다.` : "근로자가 확인했습니다.",
    },
    authority: {
      title: "감독 기관 확인",
      current: "감독 기관이 계약 내용을 확인하고 있습니다.",
      done: "감독 기관이 확인했습니다.",
    },
    confirm: {
      title: "확정 · 확인번호 발급",
      current: "확인한 내용을 블록체인에 기록하고 있습니다.",
      failed:
        "블록체인 기록에 실패했습니다. 다시 기록되면 확인번호를 알려 드립니다.",
      waiting: "기관 확인이 끝나면 블록체인에 기록합니다.",
      done: "블록체인에 기록되고 확인번호가 발급되었습니다.",
    },
  },
  stepStatuses: {
    current: "진행 중",
    failed: "실패",
    waiting: "대기",
  },
  progressHint: {
    recording:
      "기록은 보통 1~2분 안에 끝납니다. 이 화면을 닫아도 계속 진행되며, 확정되면 '내 계약'에서 확인번호를 볼 수 있습니다.",
    authority:
      "기관 확인에는 시간이 걸릴 수 있습니다. 이 화면을 닫아도 계속 진행되며, 확정되면 '내 계약'에서 확인번호를 볼 수 있습니다.",
  },

  reviewAgain: "확인한 내용 다시 보기",
  toContracts: "내 계약으로",
  lookup: "확인번호로 조회하기",
};

export type ContractReviewDoneLabels = typeof CONTRACT_REVIEW_DONE_LABELS;
