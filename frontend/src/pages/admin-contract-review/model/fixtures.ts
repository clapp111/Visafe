import type { components } from "@/shared/api";

type Detail = components["schemas"]["AdminContractDetailResponse"];
type Field = components["schemas"]["ContractDraftResponseField"];

const ai = (
  key: string,
  label: string,
  value: string | number,
  type: Field["type"],
  pageNo: number,
): Field => ({
  key,
  label,
  value,
  type,
  required: true,
  aiValue: String(value),
  valueSource: "AI",
  extractionStatus: "READ",
  sourceClause: { clauseId: 1, clauseNo: null, pageNo },
});

/** 화면 확인용 D2 응답 (와이어프레임 예시값). API 연동 단계에서 지운다. */
const FIELDS: Field[] = [
  ai("employerName", "사업체명", "한빛정밀", "STRING", 1),
  ai("representativeName", "대표자", "김민수", "STRING", 1),
  ai(
    "employerAddress",
    "사업장 주소",
    "경기도 안산시 단원구 ○○로 12",
    "STRING",
    1,
  ),
  ai("workerName", "근로자 성명", "Nguyễn Văn An", "STRING", 1),
  ai("workerNationality", "국적", "베트남", "STRING", 1),
  ai("startDate", "계약 시작일", "2027-01-02", "DATE", 1),
  ai("endDate", "계약 종료일", "2029-12-31", "DATE", 1),
  ai("workPlace", "근무 장소", "사업장 주소와 같음", "STRING", 1),
  {
    ...ai("jobDescription", "업무 내용", "금속 부품 가공 및 포장", "STRING", 1),
    aiValue: "금속 부품 가공",
    valueSource: "EMPLOYER_EDIT",
  },
  ai("workStartTime", "시업 시각", "09:00", "TIME", 1),
  ai("workEndTime", "종업 시각", "18:00", "TIME", 1),
  {
    ...ai("breakMinutes", "휴게시간(분)", 60, "INTEGER", 1),
    aiValue: null,
    valueSource: "EMPLOYER_INPUT",
    extractionStatus: "UNREAD",
    sourceClause: undefined,
  },
  ai("workDaysPerWeek", "주 근무일 수", 5, "INTEGER", 1),
  ai("workDaysText", "근무일 원문 표현", "월~금", "STRING", 1),
  ai("weeklyHoliday", "주휴일", "매주 일요일", "STRING", 1),
  {
    ...ai("monthlyBaseWage", "월 기본급(원)", 2000000, "INTEGER", 2),
    warning: {
      code: "MIN_WAGE",
      message:
        "시급 환산 약 9,569원으로 2026년 최저임금 10,320원보다 낮습니다.",
      detail: { year: 2026, hourlyWage: 9569, minimumWage: 10320 },
    },
  },
  ai("payDay", "임금 지급일", 10, "INTEGER", 2),
  ai("payMethod", "지급 방법", "본인 명의 계좌 입금", "STRING", 2),
  ai("accommodation", "숙식 제공", "숙소 제공 · 근로자 부담 없음", "STRING", 2),
];

/** ?state=reviewed 이면 이미 확정한 계약 (확정·반려 버튼 대신 안내) */
export function getAdminReviewFixture(
  contractId: number,
  state: string | null | undefined,
): Detail {
  return {
    contractId,
    contractNo: "C-2026-04817",
    stage: state === "reviewed" ? "DONE" : "AUTHORITY_REVIEW",
    versionNo: 1,
    employer: { companyName: "한빛정밀", representativeName: "김민수" },
    worker: { memberId: 2, name: "Nguyễn Văn An", nationality: "VN" },
    // 시드 계약 4 원본 (원본 미리보기는 실제 M3를 부른다)
    file: {
      fileId: 6,
      fileName: "contract_sample.pdf",
      mimeType: "application/pdf",
      sizeBytes: 1_150_000,
      pageCount: 2,
    },
    fields: FIELDS,
    editLogs: [
      {
        key: "jobDescription",
        before: "금속 부품 가공",
        after: "금속 부품 가공 및 포장",
        editedAt: "2026-11-13T04:18:00Z",
      },
      {
        key: "breakMinutes",
        before: null,
        after: "60",
        editedAt: "2026-11-13T04:20:00Z",
      },
    ],
    timeline: [
      {
        type: "SUBMITTED",
        occurredAt: "2026-11-13T04:24:00Z",
        actorLabel: "한빛정밀",
      },
      {
        type: "TRANSLATION_COMPLETED",
        occurredAt: "2026-11-13T04:30:00Z",
        actorLabel: null,
      },
      {
        type: "WORKER_UNDERSTOOD",
        occurredAt: "2026-11-13T05:05:00Z",
        actorLabel: "Nguyễn Văn An",
        detail: { language: "vi" },
      },
      {
        type: "AUTHORITY_REVIEW_REQUESTED",
        occurredAt: "2026-11-13T05:05:01Z",
        actorLabel: null,
      },
      ...(state === "reviewed"
        ? ([
            {
              type: "AUTHORITY_APPROVED",
              occurredAt: "2026-11-13T06:12:00Z",
              actorLabel: "감독 기관",
            },
            {
              type: "CONFIRMED",
              occurredAt: "2026-11-13T06:14:00Z",
              actorLabel: null,
            },
          ] as const)
        : []),
    ],
  };
}
