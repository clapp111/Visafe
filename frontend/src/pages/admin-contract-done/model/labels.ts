/** 감독 기관 확정 완료 문구. 화면 언어(#77)가 붙으면 언어별 값으로 바꾼다. */
export const ADMIN_CONTRACT_DONE_LABELS = {
  breadcrumb: "현재 위치",
  home: "홈",
  list: "확인 대기 목록",
  current: "확정 완료",
  loading: "확정 진행 상황을 불러오는 중",
  loadError: "확정 진행 상황을 불러오지 못했습니다",

  title: {
    recording: "확정했습니다",
    done: "계약이 확정되었습니다",
  },
  description: {
    recording:
      "블록체인에 기록하고 있습니다. 기록이 끝나면 확인번호가 발급되고 고용주와 근로자에게 알림이 갑니다.",
    done: "블록체인 기록이 끝나 확인번호가 발급되었습니다. 고용주와 근로자에게 알림을 보냈습니다.",
  },

  confirmationNo: "확인번호",
  copy: "번호 복사",
  copied: "복사했습니다",
  lookup: "조회 화면에서 보기",

  chainTitle: "블록체인 기록",
  chainStatus: "상태",
  chainStatuses: {
    PENDING: "기록 대기",
    SENT: "기록 중",
    MINED: "기록 완료",
    FAILED: "기록 실패",
  },
  confirmedAt: "확정 일시",
  network: "네트워크",
  networkNames: { sepolia: "Sepolia 테스트넷" } as Record<string, string>,
  txHash: "트랜잭션 해시",
  explorer: "탐색기에서 보기 (새 창)",
  chainHint:
    "기록이 늦어지면 상태가 '기록 중'으로 표시되고, 끝나는 즉시 확인번호가 발급됩니다.",

  progressTitle: "진행 상황",
  steps: {
    submitted: "고용주 제출",
    translation: "번역",
    worker: "근로자 확인",
    authority: "감독 기관 확인",
    confirm: "확정 · 확인번호 발급",
  },
  stepStatuses: {
    done: "완료",
    current: "진행 중",
    failed: "실패",
    waiting: "대기",
  },

  toList: "확인 대기 목록으로",
};

export type AdminContractDoneLabels = typeof ADMIN_CONTRACT_DONE_LABELS;
