import type { TermValueLabels } from "@/entities/contract";
import type { components } from "@/shared/api";

type Detail = components["schemas"]["ContractDetailResponse"];
type Stage = NonNullable<Detail["stage"]>;
type Role = "EMPLOYER" | "WORKER";

/** 단계 안내. 문구가 역할마다 다르면 역할별로 둔다 */
type StageCopy = {
  title: string;
  description: string | Record<Role, string>;
};

const AWAITING_REGISTRATION = {
  EMPLOYER: "원본과 비교해 항목을 확인한 뒤 제출해 주세요.",
  WORKER: "고용주가 등록을 마치고 번역이 끝나면 확인 요청이 옵니다.",
};

/** 계약 상세 문구. 화면 언어(#77)가 붙으면 언어별 값으로 바꾼다. */
export const CONTRACT_DETAIL_LABELS = {
  breadcrumb: "현재 위치",
  home: "홈",
  contracts: "내 계약",
  current: "계약 상세",
  loading: "계약을 불러오는 중",
  loadError: "계약을 불러오지 못했습니다",
  previousContract: "이 계약은 이전 계약을 대체합니다.",
  previousContractLink: "이전 계약 보기",

  statuses: {
    IN_PROGRESS: "진행",
    CONFIRMED: "확정",
    TERMINATED: "종료",
  } satisfies Record<NonNullable<Detail["status"]>, string>,
  stages: {
    UPLOADED: {
      title: "고용주가 계약을 등록하고 있습니다",
      description: AWAITING_REGISTRATION,
    },
    REVIEWING: {
      title: "고용주가 계약 내용을 검토하고 있습니다",
      description: AWAITING_REGISTRATION,
    },
    TRANSLATING: {
      title: "계약 내용을 번역하고 있습니다",
      description: {
        EMPLOYER: "번역이 끝나면 근로자에게 확인 요청이 전달됩니다.",
        WORKER: "번역이 끝나면 확인 요청이 옵니다.",
      },
    },
    AWAITING_WORKER: {
      title: "근로자 확인을 기다리고 있습니다",
      description: {
        EMPLOYER:
          "근로자가 내용을 이해했다고 응답하면 블록체인에 기록되고 확인번호가 발급됩니다.",
        WORKER: "계약 내용을 읽고 이해했는지 알려 주세요.",
      },
    },
    RETURNED: {
      title: "계약이 고용주에게 돌아왔습니다",
      description: {
        EMPLOYER:
          "근로자 신고나 기관 반려로 돌아왔습니다. 원본과 다른 부분을 고친 뒤 다시 제출해 주세요.",
        WORKER: "고용주가 고친 뒤 다시 확인을 요청하면 알려 드립니다.",
      },
    },
    AUTHORITY_REVIEW: {
      title: "감독 기관이 확인하고 있습니다",
      description:
        "기관 확인이 끝나면 블록체인에 기록되고 확인번호가 발급됩니다.",
    },
    RECORDING: {
      title: "블록체인에 기록하고 있습니다",
      description: "기록이 끝나면 계약이 확정되고 확인번호가 발급됩니다.",
    },
    DONE: {
      title: "현재 유효한 계약입니다",
      description: "",
    },
    ENDED: {
      title: "종료된 계약입니다",
      description: "",
    },
  } satisfies Record<Stage, StageCopy>,
  confirmed: (dateTime: string) => `${dateTime} 확정 · 확인번호 `,
  terminated: (dateTime: string) => `${dateTime} 종료`,

  copyConfirmation: "확인번호 복사",
  copied: "복사했습니다",
  lookup: "조회 화면에서 보기",
  download: "등록된 원본 내려받기",
  downloading: "내려받는 중",
  downloadError: "등록된 원본을 내려받지 못했습니다",
  editDraft: "이어서 검토",
  retry: "다시 시도하러 가기",
  respondReview: "계약 내용 확인하기",
  startRevision: "수정하기",
  revising: "수정을 시작하는 중",
  revisionError: "계약 수정을 시작하지 못했습니다",

  viewerHint:
    "'등록된 원본 내려받기'로 받은 파일은 나중에 조회 화면에서 대조할 수 있습니다. 파일을 다시 저장하거나 스캔하면 대조할 수 없으니 받은 그대로 보관하세요.",
  viewerPreview: (page: number) => `원본 계약서 ${page}쪽 미리보기`,
  viewerDownload: "원본 내려받기",

  termsTitle: "계약 내용",
  highlightWord: "형광펜",
  highlightLegend: "고용주가 직접 입력한 값",
  moreTerms: (count: number) => `항목 ${count}개 더 보기`,
  lessTerms: "항목 접기",
  noValue: "계약서에 없음",
  units: {
    won: (amount: string) => `${amount}원`,
    minutes: (minutes: number) => `${minutes}분`,
    daysPerWeek: (days: number) => `주 ${days}일`,
    payDay: (day: number) => `매월 ${day}일`,
  },

  historyTitle: "확인 이력",
  chainTitle: "블록체인 기록",

  termination: {
    sectionTitle: "계약을 끝내야 하나요?",
    sectionDescription:
      "계약기간이 끝나면 자동으로 종료됩니다. 기간 전에 끝내려면 종료 요청을 보내고, 상대방이 동의하면 종료가 기록됩니다. 확정된 계약 내용은 바꿀 수 없으며, 조건이 바뀌면 새 계약을 맺습니다.",
    request: "종료 요청",

    requestedByOther: (name: string) => `${name} 님이 계약 종료를 요청했습니다`,
    requestedByOtherFallback: "상대방이 계약 종료를 요청했습니다",
    requestedByOtherHint:
      "동의하면 마지막 근무일 다음 날 계약이 끝나고 블록체인에 종료가 기록됩니다. 거절하면 계약이 그대로 유지됩니다.",
    requestedByMe: "종료 요청을 보냈습니다",
    requestedByMeHint:
      "상대방의 응답을 기다리고 있습니다. 동의하기 전까지는 요청을 취소할 수 있습니다.",
    agreed: "계약 종료에 합의했습니다",
    agreedHint: (endDate: string) =>
      `${endDate}까지 근무하고, 다음 날 계약이 끝납니다.`,
    lastWorkDay: "마지막 근무일",
    reason: "종료 사유",
    noReason: "적지 않음",
    agree: "동의",
    decline: "거절",
    cancel: "요청 취소",
    responding: "처리하는 중",

    dialogTitle: "계약 종료를 요청할까요?",
    dialogDescription:
      "상대방이 동의하면 마지막 근무일 다음 날 계약이 끝나고 블록체인에 종료가 기록됩니다.",
    lastWorkDayHint: (range: string) => `${range} 사이에서 고를 수 있습니다.`,
    lastWorkDayRequired: "마지막 근무일을 골라 주세요.",
    lastWorkDayInvalid:
      "마지막 근무일은 오늘부터 계약 종료일까지 고를 수 있습니다.",
    reasonOptional: "(선택)",
    reasonHint: "상대방에게 함께 전달됩니다.",
    close: "취소",
    send: "종료 요청 보내기",
    sending: "보내는 중",

    agreeTitle: "계약 종료에 동의할까요?",
    agreeDescription: (endDate: string) =>
      `동의하면 ${endDate}까지 근무하고 다음 날 계약이 끝납니다. 블록체인에 종료가 기록되며 되돌릴 수 없습니다.`,
    agreeConfirm: "동의",
  },
} satisfies TermValueLabels & Record<string, unknown>;

export type ContractDetailLabels = typeof CONTRACT_DETAIL_LABELS;
export type ContractRole = Role;
