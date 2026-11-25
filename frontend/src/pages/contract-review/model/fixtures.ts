import type { components } from "@/shared/api";

type Question = components["schemas"]["QuestionResponse"];

/** 질문 영역 확인용 임시 데이터 (와이어프레임 예시). API 연동 후 지운다. */
export const QUESTIONS_FIXTURE: Question[] = [
  {
    questionId: 9,
    language: "ko",
    question: "야근하면 돈을 더 받나요?",
    answer:
      "네. 계약서에는 연장·야간·휴일에 일하면 법에 따라 추가 수당을 준다고 적혀 있습니다. 정확한 금액이나 계산 방법은 계약서에 없으니 고용노동부 상담센터(1350)에 문의하세요.",
    referredToCenter: true,
    citations: [
      {
        clauseId: 215,
        clauseNo: "제8조",
        pageNo: 2,
        quotedText:
          "연장·야간·휴일근로 시 근로기준법에 따라 가산수당을 지급한다",
      },
    ],
    askedAt: "2026-11-13T04:40:00Z",
    answeredAt: "2026-11-13T04:40:06Z",
  },
];
