import type { FieldGroupId } from "@/entities/contract";
import type { components } from "@/shared/api";
import type { UiLanguage } from "@/shared/i18n";

type Review = components["schemas"]["ContractReviewResponse"];

const ko = {
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

export type ContractReviewLabels = typeof ko;

/** 근로자 계약 내용 확인 문구 */
export const CONTRACT_REVIEW_LABELS = {
  ko,
  vi: {
    breadcrumb: "Vị trí hiện tại",
    home: "Trang chủ",
    contracts: "Hợp đồng của tôi",
    title: "Xác nhận nội dung hợp đồng lao động",
    description:
      "Người sử dụng lao động đã yêu cầu bạn xác nhận hợp đồng lao động. Hãy đọc kỹ nội dung và cho biết bạn đã hiểu hay chưa.",
    loading: "Đang tải nội dung hợp đồng",
    loadError: "Không thể tải nội dung hợp đồng",

    summary: {
      employer: "Người sử dụng lao động",
      requestedAt: "Thời điểm yêu cầu",
      contractNo: "Số hợp đồng",
      status: "Trạng thái",
    },
    statuses: {
      IN_PROGRESS: "Đang xử lý",
      CONFIRMED: "Đã xác nhận",
      TERMINATED: "Đã chấm dứt",
    },

    viewerHint: "Hãy so sánh phần giải thích các mục với bản gốc.",
    viewerPreview: (page: number) => `Xem trước trang ${page} của hợp đồng gốc`,
    viewerDownload: "Tải xuống bản gốc",

    keyTermsTitle: "3 điều cần kiểm tra",
    keyTerms: {
      wage: "Lương cơ bản hằng tháng",
      hours: "Giờ làm việc",
      period: "Thời hạn hợp đồng",
    },
    needsCheck: "Cần kiểm tra",
    weeklyHours: (hours: number) => `${hours} giờ/tuần`,
    breakTime: "Nghỉ giải lao",
    aboutYears: (years: number) => `khoảng ${years} năm`,
    aboutMonths: (months: number) => `khoảng ${months} tháng`,
    noEndDate: "Không xác định thời hạn",

    minWageTitle: "Lương cơ bản hằng tháng có thể thấp hơn mức lương tối thiểu",
    minWageHelp:
      "Kết quả có thể khác tùy việc có tính tiền ăn ở hay không. Nếu có thắc mắc, hãy liên hệ Trung tâm tư vấn của Bộ Việc làm và Lao động Hàn Quốc (gọi 1350, không cần mã vùng).",

    groups: {
      parties: "Các bên trong hợp đồng",
      period: "Thời hạn hợp đồng · Nơi làm việc",
      hours: "Giờ làm việc · Ngày nghỉ",
      wage: "Tiền lương · Ăn ở",
    },
    employerInput: "Người sử dụng lao động nhập",
    employerInputOpen: "Xem chi tiết phần người sử dụng lao động nhập",
    employerInputClose: "Thu gọn phần người sử dụng lao động nhập",
    aiValue: "Giá trị AI đọc từ bản gốc:",
    aiUnread: "Không đọc được",
    employerValue: "Giá trị người sử dụng lao động nhập:",
    showSource: (pageNo: number) => `Xem trang ${pageNo} bản gốc`,
    noValue: "Không có trong hợp đồng",
    units: {
      won: (amount: string) => `${amount} won`,
      minutes: (minutes: number) => `${minutes} phút`,
      daysPerWeek: (days: number) => `${days} ngày/tuần`,
      payDay: (day: number) => `ngày ${day} hằng tháng`,
    },

    clausesTitle: (count: number) =>
      `Nguyên văn và bản dịch theo điều khoản (${count})`,
    clausesOpen: "Mở các điều khoản",
    clausesClose: "Thu gọn các điều khoản",
    clausesHint:
      "Đây là nguyên văn câu chữ trong hợp đồng và bản dịch. Nếu phần giải thích các mục ở trên khác với nguyên văn, hãy báo bằng nút Khác với bản gốc.",
    clauseTitle: (seq: number, clauseNo?: string | null) =>
      clauseNo
        ? /^\d+$/.test(clauseNo)
          ? `Điều ${clauseNo}`
          : clauseNo
        : `Câu thứ ${seq}`,
    original: "Nguyên văn",
    translation: "Bản dịch",
    noTranslation: "Chưa có bản dịch.",

    questionsTitle: "Hỏi về hợp đồng",
    questionsHint:
      "Câu trả lời dựa trên nội dung của hợp đồng này và hiển thị kèm câu nguyên văn làm căn cứ. Với nội dung cần đánh giá pháp lý, hãy liên hệ trung tâm tư vấn.",
    questionSuggestions: [
      "Làm thêm giờ có được trả thêm tiền không?",
      "Tôi được nghỉ phép bao nhiêu ngày?",
      "Tôi có thể chấm dứt hợp đồng giữa chừng không?",
    ],
    questionsList: "Câu hỏi và câu trả lời",
    questionInput: "Nhập câu hỏi",
    questionPlaceholder: "Nhập điều bạn muốn hỏi",
    ask: "Hỏi",
    answering: "Đang tạo câu trả lời (tối đa 20 giây)",
    answerFailed:
      "Không thể tạo câu trả lời. Hãy hỏi lại sau ít phút hoặc liên hệ Trung tâm tư vấn của Bộ Việc làm và Lao động Hàn Quốc (gọi 1350, không cần mã vùng).",
    centerNotice:
      "Để biết thêm chi tiết, hãy liên hệ Trung tâm tư vấn của Bộ Việc làm và Lao động Hàn Quốc (gọi 1350, không cần mã vùng).",

    confirmTitle: "Bạn đã hiểu nội dung chưa?",
    confirmDescription: [
      "Khi bấm Tôi đã hiểu, ",
      "hợp đồng sẽ được xác nhận theo nội dung bạn vừa xem và ghi lên blockchain; sau đó không thể thay đổi nội dung.",
      " Nếu điều kiện lao động thay đổi, bạn sẽ xác nhận lại bằng hợp đồng mới. Nếu có phần khác với hợp đồng gốc, hãy bấm Khác với bản gốc để báo.",
    ],
    confirmCheck: "Tôi đã đọc toàn bộ nội dung hợp đồng và đã hiểu.",
    confirmRequired: "Hãy đánh dấu sau khi đã đọc toàn bộ nội dung hợp đồng.",
    report: "Khác với bản gốc",
    understood: "Tôi đã hiểu",
    submitting: "Đang gửi",
    responseError: "Không thể gửi câu trả lời",
    reload: "Tải nội dung mới nhất",

    cannotRespondTitle: "Hiện không thể trả lời hợp đồng này",
    cannotRespondDescription:
      "Bạn đã trả lời rồi hoặc người sử dụng lao động đang sửa nội dung. Hãy xem tiến trình tại Chi tiết hợp đồng.",
    contractDetail: "Xem chi tiết hợp đồng",

    reportTitle: "Hãy cho biết phần khác với bản gốc",
    reportDescription:
      "Hãy chọn các mục khác với hợp đồng gốc. Thông tin sẽ được gửi cho người sử dụng lao động và hợp đồng sẽ quay lại bước người sử dụng lao động kiểm tra.",
    reportItemsLegend: "Các mục khác (có thể chọn nhiều)",
    reportChoices: {
      employer: "Người sử dụng lao động (chủ doanh nghiệp)",
      employerAddress: "Địa chỉ nơi làm việc của doanh nghiệp",
      worker: "Người lao động",
      period: "Thời hạn hợp đồng",
      workPlace: "Nơi làm việc",
      jobDescription: "Nội dung công việc",
      workHours: "Giờ làm việc",
      breakMinutes: "Thời gian nghỉ giải lao",
      weeklyHoliday: "Ngày nghỉ hằng tuần",
      monthlyBaseWage: "Lương cơ bản hằng tháng",
      payment: "Ngày trả lương · Cách trả lương",
      accommodation: "Cung cấp chỗ ăn ở",
    },
    reportItemsRequired: "Hãy chọn ít nhất một mục khác với bản gốc.",
    reportReason: "Khác ở điểm nào?",
    reportReasonOptional: "(không bắt buộc)",
    reportReasonHint:
      "Bạn có thể viết bằng tiếng Việt. Người sử dụng lao động sẽ nhận được kèm bản dịch tiếng Hàn.",
    cancel: "Hủy",
    send: "Gửi",

    reportedTitle: "Đã gửi phần khác với bản gốc cho người sử dụng lao động",
    reportedDescription:
      "Khi người sử dụng lao động so sánh với bản gốc, sửa lại và yêu cầu xác nhận lại, việc cần xác nhận sẽ hiện tại Hợp đồng của tôi.",
    toContracts: "Đến Hợp đồng của tôi",
  },
} satisfies Record<UiLanguage, ContractReviewLabels>;
