import type { components } from "@/shared/api";

type ContractDraftResponse = components["schemas"]["ContractDraftResponse"];
type Field = components["schemas"]["ContractDraftResponseField"];

// 화면 확인용 와이어프레임 예시값. R4 검토 항목 조회를 연결하면 지운다.
function field(
  key: string,
  label: string,
  type: Field["type"],
  value: Field["value"],
  extra: Partial<Field> = {},
): Field {
  return {
    key,
    label,
    type,
    required: true,
    value,
    aiValue: value === null ? null : String(value),
    valueSource: "AI",
    extractionStatus: value === null ? "UNREAD" : "READ",
    warning: undefined,
    sourceClause: { clauseId: 1, clauseNo: null, pageNo: 1 },
    ...extra,
  };
}

const MIN_WAGE_MESSAGE =
  "주 40시간(월 209시간) 기준 환산 시급 약 9,569원으로, 2026년 최저임금 시급 10,320원보다 낮습니다. 산입 범위에 따라 달라질 수 있으니 다시 확인해 주세요.";

function fields(breakMinutes: Field): Field[] {
  return [
    field("employerName", "사업체명", "STRING", "한빛정밀"),
    field("representativeName", "대표자", "STRING", "김민수"),
    field(
      "employerAddress",
      "사업장 주소",
      "STRING",
      "경기도 안산시 단원구 ○○로 12",
    ),
    field("workerName", "근로자 성명", "STRING", "Nguyễn Văn An"),
    field("workerNationality", "국적", "STRING", "베트남", { required: false }),
    field("startDate", "계약 시작일", "DATE", "2027-01-02", {
      sourceClause: { clauseId: 3, clauseNo: "제1조", pageNo: 1 },
    }),
    field("endDate", "계약 종료일", "DATE", "2029-12-31", {
      required: false,
      sourceClause: { clauseId: 3, clauseNo: "제1조", pageNo: 1 },
    }),
    field("workPlace", "근무 장소", "STRING", "사업장 주소와 동일"),
    field("jobDescription", "업무 내용", "STRING", "금속 부품 가공 및 포장", {
      aiValue: "금속 부품 가공",
      valueSource: "EMPLOYER_EDIT",
    }),
    field("workStartTime", "시업 시각", "TIME", "09:00", {
      sourceClause: { clauseId: 5, clauseNo: "제4조", pageNo: 1 },
    }),
    field("workEndTime", "종업 시각", "TIME", "18:00", {
      sourceClause: { clauseId: 5, clauseNo: "제4조", pageNo: 1 },
    }),
    breakMinutes,
    field("workDaysPerWeek", "주 근무일 수", "INTEGER", 5),
    field("workDaysText", "근무일 원문 표현", "STRING", "월~금", {
      required: false,
    }),
    field("weeklyHoliday", "주휴일", "STRING", "일요일"),
    field("monthlyBaseWage", "월 기본급(원)", "INTEGER", 2000000, {
      warning: { code: "MIN_WAGE", message: MIN_WAGE_MESSAGE },
      sourceClause: { clauseId: 8, clauseNo: "제6조", pageNo: 2 },
    }),
    field("payDay", "임금 지급일", "INTEGER", 10, {
      sourceClause: { clauseId: 8, clauseNo: "제6조", pageNo: 2 },
    }),
    field("payMethod", "지급 방법", "STRING", "근로자 명의 계좌 입금"),
    field(
      "accommodation",
      "숙식 제공",
      "STRING",
      "숙소 제공, 근로자 부담 없음",
      {
        required: false,
        sourceClause: { clauseId: 9, clauseNo: "제7조", pageNo: 2 },
      },
    ),
  ];
}

const BASE: ContractDraftResponse = {
  contractId: 1,
  contractNo: "C-2026-04817",
  stage: "REVIEWING",
  versionNo: 1,
  processing: undefined,
  worker: undefined,
  file: {
    fileId: 1,
    fileName: "표준근로계약서_한빛정밀_가상.pdf",
    mimeType: "application/pdf",
    sizeBytes: 1258291,
    pageCount: 2,
  },
  fields: fields(
    field("breakMinutes", "휴게시간(분)", "INTEGER", null, {
      sourceClause: undefined,
    }),
  ),
  attention: [
    { key: "breakMinutes", reason: "UNREAD" },
    { key: "monthlyBaseWage", reason: "MIN_WAGE" },
  ],
  returned: undefined,
};

const RETURNED: ContractDraftResponse = {
  ...BASE,
  versionNo: 2,
  fields: fields(
    field("breakMinutes", "휴게시간(분)", "INTEGER", 60, {
      sourceClause: { clauseId: 5, clauseNo: "제4조", pageNo: 1 },
    }),
  ),
  attention: [
    { key: "breakMinutes", reason: "REPORTED" },
    { key: "monthlyBaseWage", reason: "MIN_WAGE" },
  ],
  returned: {
    by: "WORKER",
    at: "2026-11-13T05:20:00Z",
    reportedFields: ["breakMinutes"],
    reason: "Bản gốc ghi là 30 phút.",
    reasonKo: "원본에는 30분이라고 적혀 있어요.",
  },
};

const EXTRACTING: ContractDraftResponse = {
  ...BASE,
  stage: "UPLOADED",
  processing: { task: "EXTRACTION", status: "RUNNING", attempt: 1 },
  fields: [],
  attention: [],
};

const FAILED: ContractDraftResponse = {
  ...EXTRACTING,
  processing: {
    task: "EXTRACTION",
    status: "FAILED",
    attempt: 3,
    lastError: "LLM 응답을 읽지 못함",
  },
};

/**
 * 주소의 계약 ID로 예시 상태를 고른다. 1 검토, 2 돌아온 계약, 3 추출 중, 4 추출 실패.
 * 원본 미리보기는 로컬 DB에 있는 계약 1의 파일을 그대로 쓴다.
 */
export function getDraftFixture(contractId: number): ContractDraftResponse {
  return [BASE, RETURNED, EXTRACTING, FAILED][contractId - 1] ?? BASE;
}
