import type { FieldGroupId } from "@/entities/contract";
import type { components } from "@/shared/api";

type Review = components["schemas"]["ContractReviewResponse"];

/** 근로자 계약 내용 확인 문구. 화면 언어(#77)가 붙으면 언어별 값으로 바꾼다. */
export const CONTRACT_REVIEW_LABELS = {
  breadcrumb: "현재 위치",
  home: "홈",
  contracts: "내 계약",
  title: "근로계약 내용 확인",
  description:
    "고용주가 근로계약서 확인을 요청했습니다. 내용을 천천히 읽고, 이해했는지 알려 주세요.",
  loading: "계약 내용을 불러오는 중",
  loadError: "계약 내용을 불러오지 못했습니다",

  summary: {
    employer: "고용주",
    requestedAt: "요청 일시",
    contractNo: "계약 번호",
    status: "상태",
  },
  statuses: {
    IN_PROGRESS: "진행",
    CONFIRMED: "확정",
    TERMINATED: "종료",
  } satisfies Record<NonNullable<Review["status"]>, string>,

  viewerHint: "항목 설명과 원본을 비교해 보세요.",
  viewerPreview: (page: number) => `원본 계약서 ${page}쪽 미리보기`,
  viewerDownload: "원본 내려받기",

  keyTermsTitle: "꼭 확인할 3가지",
  keyTerms: {
    wage: "월 기본급",
    hours: "근로시간",
    period: "계약기간",
  },
  needsCheck: "확인 필요",
  weeklyHours: (hours: number) => `주 ${hours}시간`,
  breakTime: "휴게",
  aboutYears: (years: number) => `약 ${years}년`,
  aboutMonths: (months: number) => `약 ${months}개월`,
  noEndDate: "기간의 정함 없음",

  minWageTitle: "월 기본급이 최저임금보다 낮을 수 있습니다",
  minWageHelp:
    "숙식비 포함 여부 등에 따라 달라질 수 있습니다. 궁금하면 고용노동부 상담센터(국번 없이 1350)에 문의하세요.",

  groups: {
    parties: "계약 당사자",
    period: "계약기간 · 근무 장소",
    hours: "근로시간 · 휴일",
    wage: "임금 · 숙식",
  } satisfies Record<FieldGroupId, string>,
  employerInput: "고용주 입력",
  employerInputOpen: "고용주 입력 자세히",
  employerInputClose: "고용주 입력 접기",
  aiValue: "AI가 원본에서 읽은 값:",
  aiUnread: "읽지 못함",
  employerValue: "고용주가 입력한 값:",
  showSource: (pageNo: number) => `원본 ${pageNo}쪽 보기`,
  noValue: "계약서에 없음",
  units: {
    won: (amount: string) => `${amount}원`,
    minutes: (minutes: number) => `${minutes}분`,
    daysPerWeek: (days: number) => `주 ${days}일`,
    payDay: (day: number) => `매월 ${day}일`,
  },

  clausesTitle: (count: number) => `조항별 원문과 번역 (${count}개)`,
  clausesOpen: "조항 펼치기",
  clausesClose: "조항 접기",
  clausesHint:
    "계약서 문장을 그대로 옮긴 원문과 번역입니다. 위 항목 설명이 원문과 다르면 '원본과 다릅니다'로 알려 주세요.",
  clauseTitle: (seq: number, clauseNo?: string | null) =>
    clauseNo
      ? /^\d+$/.test(clauseNo)
        ? `제${clauseNo}조`
        : clauseNo
      : `${seq}번째 문장`,
  original: "원문",
  translation: "번역",
  noTranslation: "번역이 없습니다.",

  questionsTitle: "계약에 대해 질문하기",
  questionsHint:
    "답변은 이 계약서 내용을 근거로 하며, 근거가 된 원문 문장을 함께 보여 줍니다. 법률 판단이 필요한 내용은 상담센터에 문의하세요.",
  questionSuggestions: [
    "야근하면 돈을 더 받나요?",
    "휴가는 며칠인가요?",
    "계약을 중간에 해지할 수 있나요?",
  ],
  questionsList: "질문과 답변",
  questionInput: "질문 입력",
  questionPlaceholder: "궁금한 점을 입력하세요",
  ask: "질문",
  answering: "답변을 만드는 중입니다 (최대 20초)",
  answerFailed:
    "답변을 만들지 못했습니다. 잠시 후 다시 질문하거나 고용노동부 상담센터(국번 없이 1350)에 문의하세요.",
  centerNotice:
    "더 자세한 내용은 고용노동부 상담센터(국번 없이 1350)에 문의하세요.",

  confirmTitle: "내용을 이해했나요?",
  confirmDescription: [
    "'이해했습니다'를 누르면 ",
    "지금 보신 내용으로 계약이 확정되어 블록체인에 기록되며, 이후에는 내용을 바꿀 수 없습니다.",
    " 근로조건이 바뀌면 새 계약으로 다시 확인합니다. 원본 계약서와 다른 부분이 있으면 '원본과 다릅니다'를 눌러 알려 주세요.",
  ],
  confirmCheck: "계약 내용을 모두 읽었고, 이해했습니다.",
  confirmRequired: "계약 내용을 모두 읽은 뒤 체크해 주세요.",
  report: "원본과 다릅니다",
  understood: "이해했습니다",
  submitting: "보내는 중",
  responseError: "응답을 보내지 못했습니다",
  reload: "최신 내용 불러오기",

  cannotRespondTitle: "지금은 응답할 수 없는 계약입니다",
  cannotRespondDescription:
    "이미 응답했거나 고용주가 내용을 고치고 있습니다. 진행 상황은 계약 상세에서 확인하세요.",
  contractDetail: "계약 상세 보기",

  reportTitle: "원본과 다른 부분을 알려 주세요",
  reportDescription:
    "원본 계약서와 다른 항목을 골라 주세요. 고용주에게 전달되고, 계약은 고용주 검토 단계로 돌아갑니다.",
  reportItemsLegend: "다른 항목 (여러 개 선택 가능)",
  reportChoices: {
    employer: "고용주(사업주)",
    employerAddress: "사업장 주소",
    worker: "근로자",
    period: "계약기간",
    workPlace: "근무 장소",
    jobDescription: "업무 내용",
    workHours: "근로시간",
    breakMinutes: "휴게시간",
    weeklyHoliday: "주휴일",
    monthlyBaseWage: "월 기본급",
    payment: "지급일 · 지급 방법",
    accommodation: "숙식 제공",
  },
  reportItemsRequired: "원본과 다른 항목을 하나 이상 골라 주세요.",
  reportReason: "무엇이 다른가요?",
  reportReasonOptional: "(선택)",
  reportReasonHint:
    "베트남어로 써도 됩니다. 고용주에게는 한국어 번역과 함께 전달됩니다.",
  cancel: "취소",
  send: "보내기",

  reportedTitle: "원본과 다른 부분을 고용주에게 보냈습니다",
  reportedDescription:
    "고용주가 원본과 비교해 고친 뒤 다시 확인을 요청하면 '내 계약'에 확인할 일로 나타납니다.",
  toContracts: "내 계약으로",
};

export type ContractReviewLabels = typeof CONTRACT_REVIEW_LABELS;
