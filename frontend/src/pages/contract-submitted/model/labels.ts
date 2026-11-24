/** 계약 등록 제출 완료 화면 문구. 화면 언어(#54)가 붙으면 언어별 값으로 바꾼다. */
export const CONTRACT_SUBMITTED_LABELS = {
  breadcrumb: "현재 위치",
  home: "홈",
  register: "계약 등록",
  loading: "제출 진행 상황을 불러오는 중",
  loadError: "제출 진행 상황을 불러오지 못했습니다",

  title: {
    translating: "확인 요청이 접수되었습니다",
    awaiting: "근로자에게 확인 요청이 전달되었습니다",
  },
  description: {
    translating: [
      "번역이 끝나면 ",
      " 님에게 확인 요청이 전달됩니다. 번역은 보통 몇 분 안에 끝나며, 이 화면을 닫아도 계속 진행됩니다.",
    ],
    awaiting: [
      "",
      " 님이 계약 내용을 확인하면 블록체인에 기록되고 확인번호가 발급됩니다.",
    ],
  },
  contractNo: "계약 번호(내부)",
  currentStatus: "현재 상태",
  inProgress: "진행",

  progressTitle: "진행 상황",
  steps: {
    submitted: {
      title: "고용주 제출",
      description: (count: number) =>
        `원본 파일과 검토한 항목 ${count}개가 저장되었습니다.`,
    },
    translation: {
      title: "번역",
      description: "핵심 항목과 쉬운 설명을 번역하고 있습니다.",
      done: "번역을 마쳤습니다.",
      failed: "번역에 3번 실패했습니다.",
    },
    worker: {
      title: "근로자 확인",
      description: "번역이 끝나면 근로자에게 알림이 갑니다.",
      current: "근로자가 계약 내용을 확인하고 있습니다.",
    },
    confirm: {
      title: "확정 · 확인번호 발급",
      description:
        "근로자가 확인하면 블록체인에 기록되고 확인번호가 발급됩니다.",
    },
  },
  statuses: {
    current: "진행 중",
    failed: "실패",
    waiting: "대기",
  },
  progressHint: [
    "번역에 실패하면 자동으로 다시 시도하며, 3회 실패하면 알림으로 알려 드립니다.",
    "근로자가 '원본과 다릅니다'를 선택하면 이 계약은 다시 고용주 검토 단계로 돌아옵니다.",
  ],

  translationFailedTitle: "번역을 마치지 못했습니다",
  translationFailedHint:
    "잠시 뒤 다시 시도해 주세요. 다시 시도하면 새로 번역을 시작합니다.",
  retryTranslation: "다시 시도",
  retrying: "다시 시도하는 중",

  newContract: "새 계약 등록",
  viewContract: "내 계약에서 보기",
};

export type ContractSubmittedLabels = typeof CONTRACT_SUBMITTED_LABELS;
