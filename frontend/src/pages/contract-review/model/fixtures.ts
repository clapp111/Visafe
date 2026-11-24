import type { components } from "@/shared/api";

type Review = components["schemas"]["ContractReviewResponse"];
type Item = components["schemas"]["ContractReviewResponseItem"];

/** 화면 확인용 W1 응답 (와이어프레임 예시값). API 연동 단계에서 지운다. */
const ITEMS: Item[] = [
  {
    key: "employerName",
    label: "사업체명",
    value: "한빛정밀",
    translatedValue: "Hanbit Precision (한빛정밀)",
    easyExplanation: "Đây là công ty tuyển dụng bạn.",
    easyExplanationKo: "나를 고용하는 회사입니다.",
    valueSource: "AI",
    sourceClause: { clauseId: 1, clauseNo: null, pageNo: 1 },
  },
  {
    key: "representativeName",
    label: "대표자",
    value: "김민수",
    translatedValue: "Kim Min-su (김민수)",
    easyExplanation: "Ông Kim Min-su đại diện công ty ký hợp đồng với bạn.",
    easyExplanationKo: "김민수 님이 회사를 대표해 계약합니다.",
    valueSource: "AI",
    sourceClause: { clauseId: 1, clauseNo: null, pageNo: 1 },
  },
  {
    key: "employerAddress",
    label: "사업장 주소",
    value: "경기도 안산시 단원구 ○○로 12",
    translatedValue: "12 ○○-ro, Danwon-gu, Ansan-si, Gyeonggi-do",
    easyExplanation: "Đây là địa chỉ của công ty.",
    easyExplanationKo: "회사가 있는 곳의 주소입니다.",
    valueSource: "AI",
    sourceClause: { clauseId: 1, clauseNo: null, pageNo: 1 },
  },
  {
    key: "workerName",
    label: "근로자 성명",
    value: "Nguyễn Văn An",
    translatedValue: "Nguyễn Văn An",
    easyExplanation: "Đây là tên của bạn. Hãy kiểm tra xem có đúng không.",
    easyExplanationKo: "이 계약을 맺는 근로자입니다. 이름이 맞는지 확인하세요.",
    valueSource: "AI",
    sourceClause: { clauseId: 2, clauseNo: null, pageNo: 1 },
  },
  {
    key: "workerNationality",
    label: "국적",
    value: "베트남",
    translatedValue: "Việt Nam",
    easyExplanation: "Đây là quốc tịch của bạn ghi trong hợp đồng.",
    easyExplanationKo: "계약서에 적힌 근로자의 국적입니다.",
    valueSource: "AI",
    sourceClause: { clauseId: 2, clauseNo: null, pageNo: 1 },
  },
  {
    key: "startDate",
    label: "계약 시작일",
    value: "2027-01-02",
    translatedValue: "02/01/2027",
    easyExplanation: "Bạn bắt đầu làm việc từ ngày 2 tháng 1 năm 2027.",
    easyExplanationKo: "2027년 1월 2일부터 일합니다.",
    valueSource: "AI",
    sourceClause: { clauseId: 3, clauseNo: "1", pageNo: 1 },
  },
  {
    key: "endDate",
    label: "계약 종료일",
    value: "2029-12-31",
    translatedValue: "31/12/2029",
    easyExplanation: "Hợp đồng kết thúc vào ngày 31 tháng 12 năm 2029.",
    easyExplanationKo: "계약은 2029년 12월 31일에 끝납니다.",
    valueSource: "AI",
    sourceClause: { clauseId: 3, clauseNo: "1", pageNo: 1 },
  },
  {
    key: "workPlace",
    label: "근무 장소",
    value: "사업장 주소와 같음",
    translatedValue: "Giống địa chỉ công ty",
    easyExplanation: "Bạn làm việc tại địa chỉ công ty ghi trong hợp đồng.",
    easyExplanationKo: "계약서에 적힌 회사 주소에서 일합니다.",
    valueSource: "AI",
    sourceClause: { clauseId: 4, clauseNo: "2", pageNo: 1 },
  },
  {
    key: "jobDescription",
    label: "업무 내용",
    value: "금속 부품 가공 및 포장",
    translatedValue: "Gia công và đóng gói linh kiện kim loại",
    easyExplanation:
      "Bạn gia công các linh kiện bằng kim loại và đóng gói linh kiện đã hoàn thành.",
    easyExplanationKo: "금속으로 된 부품을 가공하고, 완성된 부품을 포장합니다.",
    valueSource: "EMPLOYER_EDIT",
    employerInput: { aiValue: "금속 부품 가공", extractionStatus: "READ" },
    sourceClause: { clauseId: 5, clauseNo: "3", pageNo: 1 },
  },
  {
    key: "workStartTime",
    label: "시업 시각",
    value: "09:00",
    translatedValue: "09:00",
    easyExplanation: "Bạn bắt đầu làm việc lúc 9 giờ sáng.",
    easyExplanationKo: "아침 9시에 일을 시작합니다.",
    valueSource: "AI",
    sourceClause: { clauseId: 6, clauseNo: "4", pageNo: 1 },
  },
  {
    key: "workEndTime",
    label: "종업 시각",
    value: "18:00",
    translatedValue: "18:00",
    easyExplanation: "Bạn kết thúc công việc lúc 6 giờ chiều.",
    easyExplanationKo: "저녁 6시에 일이 끝납니다.",
    valueSource: "AI",
    sourceClause: { clauseId: 6, clauseNo: "4", pageNo: 1 },
  },
  {
    key: "breakMinutes",
    label: "휴게시간(분)",
    value: 60,
    translatedValue: "60 phút",
    easyExplanation:
      "Bạn được nghỉ 1 giờ trong lúc làm việc. Thời gian này không tính vào giờ làm việc.",
    easyExplanationKo:
      "일하는 중간에 1시간 쉽니다. 이 시간은 근로시간에 들어가지 않습니다.",
    valueSource: "EMPLOYER_INPUT",
    employerInput: { aiValue: null, extractionStatus: "UNREAD" },
    sourceClause: { clauseId: 6, clauseNo: "4", pageNo: 1 },
  },
  {
    key: "workDaysPerWeek",
    label: "주 근무일 수",
    value: 5,
    translatedValue: "5 ngày/tuần",
    easyExplanation: "Bạn làm việc 5 ngày một tuần.",
    easyExplanationKo: "일주일에 5일 일합니다.",
    valueSource: "AI",
    sourceClause: { clauseId: 6, clauseNo: "4", pageNo: 1 },
  },
  {
    key: "workDaysText",
    label: "근무일 원문 표현",
    value: "월~금",
    translatedValue: "Thứ Hai ~ thứ Sáu",
    easyExplanation: "Bạn làm việc từ thứ Hai đến thứ Sáu.",
    easyExplanationKo: "월요일부터 금요일까지 일합니다.",
    valueSource: "AI",
    sourceClause: { clauseId: 6, clauseNo: "4", pageNo: 1 },
  },
  {
    key: "weeklyHoliday",
    label: "주휴일",
    value: "매주 일요일",
    translatedValue: "Chủ nhật hằng tuần",
    easyExplanation: "Chủ nhật là ngày nghỉ của bạn.",
    easyExplanationKo: "일요일은 쉬는 날입니다.",
    valueSource: "AI",
    sourceClause: { clauseId: 7, clauseNo: "5", pageNo: 1 },
  },
  {
    key: "monthlyBaseWage",
    label: "월 기본급(원)",
    value: 2000000,
    translatedValue: "2.000.000 won",
    easyExplanation:
      "Mỗi tháng bạn nhận 2.000.000 won lương cơ bản. Hãy xem thêm thông báo về lương tối thiểu ở phía trên.",
    easyExplanationKo:
      "한 달에 기본급으로 200만 원을 받습니다. 상단의 최저임금 안내를 함께 확인하세요.",
    valueSource: "AI",
    sourceClause: { clauseId: 8, clauseNo: "6", pageNo: 2 },
  },
  {
    key: "payDay",
    label: "임금 지급일",
    value: 10,
    translatedValue: "Ngày 10 hằng tháng",
    easyExplanation: "Lương được trả vào ngày 10 hằng tháng.",
    easyExplanationKo: "매달 10일에 임금을 받습니다.",
    valueSource: "AI",
    sourceClause: { clauseId: 8, clauseNo: "6", pageNo: 2 },
  },
  {
    key: "payMethod",
    label: "지급 방법",
    value: "본인 명의 계좌 입금",
    translatedValue: "Chuyển vào tài khoản đứng tên bạn",
    easyExplanation:
      "Tiền lương được chuyển vào tài khoản ngân hàng đứng tên bạn.",
    easyExplanationKo: "내 이름으로 된 은행 계좌로 돈이 들어옵니다.",
    valueSource: "AI",
    sourceClause: { clauseId: 8, clauseNo: "6", pageNo: 2 },
  },
  {
    key: "accommodation",
    label: "숙식 제공",
    value: "숙소 제공 · 근로자 부담 없음",
    translatedValue: "Có chỗ ở · người lao động không phải trả tiền",
    easyExplanation: "Công ty cho bạn chỗ ở và không trừ tiền chỗ ở vào lương.",
    easyExplanationKo:
      "회사가 숙소를 주고, 숙소 비용을 월급에서 빼지 않습니다.",
    valueSource: "AI",
    sourceClause: { clauseId: 9, clauseNo: "7", pageNo: 2 },
  },
];

const CLAUSES: Review["clauses"] = [
  {
    clauseId: 1,
    seq: 1,
    clauseNo: null,
    pageNo: 1,
    textKo:
      "사용자: 업체명 한빛정밀, 소재지 경기도 안산시 단원구 ○○로 12, 대표자 김민수",
    translatedText:
      "Người sử dụng lao động: Tên công ty Hanbit Precision (한빛정밀), địa chỉ 12 ○○-ro, Danwon-gu, Ansan-si, Gyeonggi-do, người đại diện Kim Min-su",
  },
  {
    clauseId: 2,
    seq: 2,
    clauseNo: null,
    pageNo: 1,
    textKo: "근로자: 성명 Nguyễn Văn An, 국적 베트남",
    translatedText: "Người lao động: Họ tên Nguyễn Văn An, quốc tịch Việt Nam",
  },
  {
    clauseId: 3,
    seq: 3,
    clauseNo: "1",
    pageNo: 1,
    textKo: "근로계약기간: 2027년 1월 2일부터 2029년 12월 31일까지",
    translatedText:
      "Thời hạn hợp đồng lao động: Từ ngày 2 tháng 1 năm 2027 đến ngày 31 tháng 12 năm 2029",
  },
  {
    clauseId: 5,
    seq: 5,
    clauseNo: "3",
    pageNo: 1,
    textKo: "업무 내용: 금속 부품 가공",
    translatedText: "Nội dung công việc: Gia công linh kiện kim loại",
  },
  {
    clauseId: 6,
    seq: 6,
    clauseNo: "4",
    pageNo: 1,
    textKo:
      "소정근로시간: 09시 00분부터 18시 00분까지 (휴게시간: 시 분 ~ 시 분), 월~금",
    translatedText:
      "Thời gian làm việc: Từ 09:00 đến 18:00 (thời gian nghỉ: từ … đến …), thứ Hai ~ thứ Sáu",
  },
  {
    clauseId: 8,
    seq: 8,
    clauseNo: "6",
    pageNo: 2,
    textKo:
      "임금: 월급 2,000,000원, 임금지급일 매월 10일, 지급방법 근로자 명의 예금통장에 입금",
    translatedText:
      "Tiền lương: Lương tháng 2.000.000 won, trả vào ngày 10 hằng tháng, chuyển vào tài khoản tiền gửi đứng tên người lao động",
  },
  {
    clauseId: 10,
    seq: 10,
    clauseNo: "8",
    pageNo: 2,
    textKo: "연장·야간·휴일근로 시 근로기준법에 따라 가산수당을 지급한다.",
    translatedText: null,
  },
];

const REVIEW: Review = {
  contractNo: "C-2026-04817",
  status: "IN_PROGRESS",
  stage: "AWAITING_WORKER",
  canRespond: true,
  employer: {
    companyName: "한빛정밀",
    representativeName: "김민수",
    memberName: "김민수",
  },
  requestedAt: "2026-11-13T04:30:00Z",
  translationId: 57,
  language: "vi",
  // 시드의 worker1 계약 4 원본 (원본 미리보기는 실제 M3를 부른다)
  file: { fileId: 6, mimeType: "application/pdf", pageCount: 2 },
  keyTerms: {
    monthlyBaseWage: 2000000,
    payDay: 10,
    payMethod: "본인 명의 계좌 입금",
    workStartTime: "09:00",
    workEndTime: "18:00",
    workDaysText: "월~금",
    weeklyHours: 40,
    breakMinutes: 60,
    startDate: "2027-01-02",
    endDate: "2029-12-31",
  },
  warnings: [
    {
      code: "MIN_WAGE",
      key: "monthlyBaseWage",
      message:
        "Tính theo 40 giờ/tuần, lương theo giờ khoảng 9.569 won, thấp hơn lương tối thiểu năm 2026 là 10.320 won/giờ.",
      messageKo:
        "주 40시간 기준으로 계산하면 1시간에 약 9,569원입니다. 2026년 최저임금은 1시간에 10,320원입니다.",
      detail: { year: 2026, hourlyWage: 9569, minimumWage: 10320 },
    },
    {
      code: "NEEDS_CHECK",
      key: "workerNationality",
      message:
        "Giá trị này có thể chưa rõ ràng. Hãy so sánh với bản hợp đồng gốc.",
      messageKo:
        "AI가 읽은 값이 흐리거나 모호할 수 있습니다. 원본과 비교해 확인해 주세요.",
      detail: null,
    },
  ],
  items: ITEMS,
  clauses: CLAUSES,
};

/** ?state=done 이면 이미 응답해 응답할 수 없는 계약 (canRespond false) */
export function getReviewFixture(contractId: number, state?: string | null) {
  const done = state === "done";
  return {
    ...REVIEW,
    contractId,
    stage: done ? "RECORDING" : REVIEW.stage,
    canRespond: !done,
  } satisfies Review;
}
