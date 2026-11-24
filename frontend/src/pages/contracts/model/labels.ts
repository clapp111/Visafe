import type { components } from "@/shared/api";

type Item = components["schemas"]["ContractListResponseItem"];

/** 내 계약 목록 문구. 화면 언어(#54)가 붙으면 언어별 값으로 바꾼다. */
export const CONTRACT_LIST_LABELS = {
  breadcrumb: "현재 위치",
  home: "홈",
  title: "내 계약",
  description: {
    EMPLOYER: "우리 사업체가 등록한 근로계약입니다.",
    WORKER:
      "내가 맺은 근로계약입니다. 계약을 누르면 내용과 확인 기록을 볼 수 있습니다.",
  },
  newContract: "새 계약 등록",
  loading: "계약 목록을 불러오는 중",
  loadError: "계약 목록을 불러오지 못했습니다",
  revisionError: "계약 수정을 시작하지 못했습니다",
  todoBanner: (count: number) => `처리할 일이 있는 계약이 ${count}건 있습니다`,
  todoBannerAction: "지금 처리하기",

  filterLabel: "상태로 거르기",
  filters: {
    all: "전체",
    IN_PROGRESS: "진행",
    CONFIRMED: "확정",
    TERMINATED: "종료",
  },

  caption: {
    EMPLOYER: "등록한 계약 목록",
    WORKER: "내 계약 목록",
  },
  columns: {
    contractNo: "계약 번호",
    counterparty: { EMPLOYER: "근로자", WORKER: "고용주" },
    period: "계약기간",
    status: "상태",
    todo: "할 일",
    action: "보기",
  },
  statuses: {
    IN_PROGRESS: "진행",
    CONFIRMED: "확정",
    TERMINATED: "종료",
  } satisfies Record<NonNullable<Item["status"]>, string>,
  noPeriod: "추출 전",
  noTodo: "없음",
  todos: {
    FIX_REQUIRED: { tag: "수정 필요", action: "수정하기" },
    REVIEW_INCOMPLETE: { tag: "검토 중", action: "이어서 검토" },
    EXTRACTION_FAILED: { tag: "추출 실패", action: "확인하기" },
    TRANSLATION_FAILED: { tag: "번역 실패", action: "확인하기" },
    CONFIRM_REQUIRED: { tag: "확인 필요", action: "확인하기" },
    TERMINATION_RESPONSE: { tag: "종료 응답 필요", action: "확인하기" },
  } satisfies Record<
    NonNullable<Item["todo"]>,
    { tag: string; action: string }
  >,
  detail: "상세",
  workerHint:
    "근로조건이 바뀌면 기존 계약은 그대로 두고 새 계약을 맺습니다. 새 계약이 확정되면 기존 계약은 종료됩니다.",

  empty: {
    EMPLOYER: "등록한 계약이 없습니다",
    WORKER: "아직 맺은 계약이 없습니다",
  },
  emptyHint: {
    EMPLOYER: "근로계약서 원본을 올려 첫 계약을 등록해 보세요.",
    WORKER: "고용주가 계약을 등록하고 확인을 요청하면 이곳에 나타납니다.",
  },
  emptyFiltered: "이 상태의 계약이 없습니다",

  pagination: "목록 쪽 이동",
  previousPage: "이전",
  nextPage: "다음",
  pageStatus: (page: number, total: number) => `${page} / ${total}쪽`,
};

export type ContractListLabels = typeof CONTRACT_LIST_LABELS;
