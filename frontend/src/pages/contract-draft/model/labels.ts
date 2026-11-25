import type { components } from "@/shared/api";
import type { UiLanguage } from "@/shared/i18n";

type Schemas = components["schemas"];

const ko = {
  breadcrumb: "현재 위치",
  home: "홈",
  register: "계약 등록",
  title: "계약 내용 검토 · 수정",
  loading: "계약 내용을 불러오는 중입니다",
  loadError: "계약 내용을 불러오지 못했습니다",
  description: [
    "AI가 원본 계약서에서 읽어 온 핵심 항목입니다.",
    "왼쪽 원본과 비교해, 원본과 다르거나 비어 있는 항목은 고쳐 주세요.",
  ],

  attentionTitle: (count: number) =>
    `확인이 필요한 항목이 ${count}개 있습니다.`,
  attentionReasons: {
    UNREAD: "원본에서 읽지 못했습니다. 직접 입력해 주세요.",
    MIN_WAGE: "최저임금보다 낮을 수 있습니다.",
    NEEDS_CHECK: "다시 확인이 필요합니다.",
    REPORTED: "근로자가 원본과 다르다고 신고했습니다.",
  } satisfies Record<
    NonNullable<Schemas["ContractDraftResponseAttention"]["reason"]>,
    string
  >,

  returnedTitle: {
    WORKER: "근로자가 원본과 다른 부분을 신고해 계약이 돌아왔습니다",
    AUTHORITY: "감독 기관이 계약을 반려했습니다",
  } satisfies Record<
    NonNullable<Schemas["ContractDraftResponseReturned"]["by"]>,
    string
  >,
  returnedAt: "돌아온 일시",
  reportedFields: "신고한 항목",
  returnedReason: {
    WORKER: "근로자 사유",
    AUTHORITY: "반려 사유",
  },
  reasonOriginal: "원문",
  returnedHint:
    "신고된 항목을 원본과 비교해 고친 뒤 다시 제출하면, 번역을 거쳐 근로자가 다시 확인합니다. 원본 파일 자체가 잘못됐다면 원본을 다시 올려 주세요.",

  viewerHint: "쪽을 넘겨서 오른쪽 항목과 비교해 주세요.",
  viewerPreview: (page: number) => `원본 계약서 ${page}쪽 미리보기`,
  viewerDownload: "원본 내려받기",

  fieldsTitle: "추출된 핵심 항목",
  required: "필수",
  groups: {
    parties: "계약 당사자",
    period: "계약기간 · 근무 장소",
    hours: "근로시간 · 휴일",
    wage: "임금 · 숙식",
  },
  partiesHint:
    "고용주(사업주)는 회사이고, 대표자는 회사를 대표해 서명한 사람입니다.",
  valueSources: {
    EMPLOYER_EDIT: "직접 수정함",
    EMPLOYER_INPUT: "직접 입력함",
  },
  reported: "근로자 신고",
  sourceClause: (pageNo: number, clauseNo?: string | null) =>
    clauseNo ? `원본 ${pageNo}쪽 · ${clauseNo}` : `원본 ${pageNo}쪽`,
  unread: "원본에서 읽지 못했습니다. 직접 입력해 주세요.",
  warningPrefix: "확인 필요:",
  aiValue: (value: string) => `AI가 읽은 값: ${value}`,

  extracting: "AI가 원본에서 항목을 읽는 중입니다",
  extractingHint: "보통 1~2분 걸립니다. 이 화면을 닫아도 계속 진행됩니다.",
  extractionFailed: "AI가 원본에서 항목을 읽지 못했습니다",
  extractionFailedHint:
    "다시 시도하거나, 글자가 잘 보이는 파일로 원본을 다시 올려 주세요.",
  retryExtraction: "다시 시도",

  confirmNotice:
    "인식된 값은 틀릴 수 있습니다. 모든 항목을 원본과 비교한 뒤 확인해 주세요.",
  confirmCheck: "위 항목이 업로드한 원본 계약서의 내용과 같음을 확인했습니다.",
  confirmRequired: "원본 계약서와 같은지 확인한 뒤 체크해 주세요.",
  confirmHint:
    "직접 수정하거나 입력한 항목은 근로자 화면에 형광펜과 '고용주 입력' 표시로 구분되며, 근로자는 인식된 값과 원본을 함께 확인할 수 있습니다.",
  reupload: "이전: 원본 다시 올리기",
  submit: "제출하고 확인 요청 보내기",
  saving: "저장하는 중",

  reuploadTitle: "원본을 다시 올릴까요?",
  reuploadDescription:
    "새 원본을 올리면 지금까지 고친 항목이 모두 지워지고, AI가 처음부터 다시 읽습니다.",
  reuploadFileLabel: "새 원본 파일",
  reuploadFileHint: "PDF, JPG, PNG · 최대 10MB",
  dropLabel: "파일을 이곳에 끌어다 놓거나",
  selectFile: "파일 선택",
  fileListLabel: "올릴 파일",
  removeFile: "삭제",
  fileRequired: "새 원본 파일을 골라 주세요.",
  fileTypeInvalid: "PDF, JPG, PNG 파일만 올릴 수 있습니다.",
  fileTooLarge: "10MB 이하 파일만 올릴 수 있습니다.",
  cancel: "취소",
  upload: "올리고 다시 읽기",
  uploading: "올리는 중",

  requiredMissing: "필수 항목입니다. 값을 입력해 주세요.",
  submitTitle: "근로자에게 확인 요청을 보낼까요?",
  submitDescription:
    "제출하면 번역이 시작되고, 번역이 끝나는 대로 근로자에게 확인 요청이 전달됩니다.",
  recipient: "받는 사람",
  termsToConfirm: "확정할 항목",
  termCount: (count: number, employerCount: number) =>
    employerCount > 0
      ? `${count}개 (고용주 입력 ${employerCount}개 포함)`
      : `${count}개`,
  remainingWarnings: (count: number) =>
    `확인 필요 항목 ${count}개가 남아 있습니다.`,
  remainingWarningsHint: (names: string) =>
    `${names} 항목은 근로자 화면에도 같은 안내가 표시됩니다.`,
  submitNote:
    "보낸 뒤 내용을 고치면 근로자의 확인이 무효가 되어 다시 확인을 받아야 합니다.",
  send: "보내기",
  sending: "보내는 중",
};

export type ContractDraftLabels = typeof ko;

/** 계약 등록 2단계 문구 */
export const CONTRACT_DRAFT_LABELS = {
  ko,
  vi: {
    breadcrumb: "Vị trí hiện tại",
    home: "Trang chủ",
    register: "Đăng ký hợp đồng",
    title: "Kiểm tra · Sửa nội dung hợp đồng",
    loading: "Đang tải nội dung hợp đồng",
    loadError: "Không thể tải nội dung hợp đồng",
    description: [
      "Đây là các mục chính AI đã đọc từ hợp đồng gốc.",
      "Hãy so sánh với bản gốc bên trái và sửa các mục khác với bản gốc hoặc còn trống.",
    ],

    attentionTitle: (count: number) => `Có ${count} mục cần kiểm tra.`,
    attentionReasons: {
      UNREAD: "Không đọc được từ bản gốc. Hãy nhập trực tiếp.",
      MIN_WAGE: "Có thể thấp hơn mức lương tối thiểu.",
      NEEDS_CHECK: "Cần kiểm tra lại.",
      REPORTED: "Người lao động đã báo là khác với bản gốc.",
    },

    returnedTitle: {
      WORKER:
        "Hợp đồng bị trả lại vì người lao động báo có phần khác với bản gốc",
      AUTHORITY: "Cơ quan giám sát đã trả lại hợp đồng",
    },
    returnedAt: "Thời điểm trả lại",
    reportedFields: "Các mục bị báo",
    returnedReason: {
      WORKER: "Lý do của người lao động",
      AUTHORITY: "Lý do trả lại",
    },
    reasonOriginal: "Nguyên văn",
    returnedHint:
      "Hãy so sánh các mục bị báo với bản gốc, sửa lại rồi gửi lại; sau khi dịch, người lao động sẽ xác nhận lại. Nếu chính tệp gốc bị sai, hãy tải lên lại bản gốc.",

    viewerHint: "Hãy lật trang để so sánh với các mục bên phải.",
    viewerPreview: (page: number) => `Xem trước trang ${page} của hợp đồng gốc`,
    viewerDownload: "Tải xuống bản gốc",

    fieldsTitle: "Các mục chính đã trích xuất",
    required: "Bắt buộc",
    groups: {
      parties: "Các bên trong hợp đồng",
      period: "Thời hạn hợp đồng · Nơi làm việc",
      hours: "Giờ làm việc · Ngày nghỉ",
      wage: "Tiền lương · Ăn ở",
    },
    partiesHint:
      "Người sử dụng lao động (chủ doanh nghiệp) là công ty; người đại diện là người ký thay mặt công ty.",
    valueSources: {
      EMPLOYER_EDIT: "Đã tự sửa",
      EMPLOYER_INPUT: "Đã tự nhập",
    },
    reported: "Người lao động báo",
    sourceClause: (pageNo: number, clauseNo?: string | null) =>
      clauseNo
        ? `Bản gốc trang ${pageNo} · ${clauseNo}`
        : `Bản gốc trang ${pageNo}`,
    unread: "Không đọc được từ bản gốc. Hãy nhập trực tiếp.",
    warningPrefix: "Cần kiểm tra:",
    aiValue: (value: string) => `Giá trị AI đọc được: ${value}`,

    extracting: "AI đang đọc các mục từ bản gốc",
    extractingHint:
      "Thường mất 1~2 phút. Quá trình vẫn tiếp tục dù bạn đóng màn hình này.",
    extractionFailed: "AI không đọc được các mục từ bản gốc",
    extractionFailedHint:
      "Hãy thử lại, hoặc tải lên lại bản gốc bằng tệp có chữ rõ ràng.",
    retryExtraction: "Thử lại",

    confirmNotice:
      "Giá trị nhận dạng có thể sai. Hãy so sánh tất cả các mục với bản gốc rồi xác nhận.",
    confirmCheck:
      "Tôi xác nhận các mục trên giống với nội dung hợp đồng gốc đã tải lên.",
    confirmRequired: "Hãy đánh dấu sau khi kiểm tra đã giống với hợp đồng gốc.",
    confirmHint:
      "Các mục tự sửa hoặc tự nhập sẽ được đánh dấu bằng tô sáng và nhãn Người sử dụng lao động nhập trên màn hình của người lao động; người lao động có thể xem cả giá trị nhận dạng và bản gốc.",
    reupload: "Trước: Tải lên lại bản gốc",
    submit: "Gửi và yêu cầu xác nhận",
    saving: "Đang lưu",

    reuploadTitle: "Tải lên lại bản gốc?",
    reuploadDescription:
      "Khi tải lên bản gốc mới, tất cả các mục đã sửa sẽ bị xóa và AI sẽ đọc lại từ đầu.",
    reuploadFileLabel: "Tệp gốc mới",
    reuploadFileHint: "PDF, JPG, PNG · tối đa 10MB",
    dropLabel: "Kéo tệp thả vào đây hoặc",
    selectFile: "Chọn tệp",
    fileListLabel: "Tệp sẽ tải lên",
    removeFile: "Xóa",
    fileRequired: "Hãy chọn tệp gốc mới.",
    fileTypeInvalid: "Chỉ có thể tải lên tệp PDF, JPG, PNG.",
    fileTooLarge: "Chỉ có thể tải lên tệp tối đa 10MB.",
    cancel: "Hủy",
    upload: "Tải lên và đọc lại",
    uploading: "Đang tải lên",

    requiredMissing: "Đây là mục bắt buộc. Hãy nhập giá trị.",
    submitTitle: "Gửi yêu cầu xác nhận cho người lao động?",
    submitDescription:
      "Khi gửi, việc dịch sẽ bắt đầu và yêu cầu xác nhận sẽ được gửi cho người lao động ngay khi dịch xong.",
    recipient: "Người nhận",
    termsToConfirm: "Các mục sẽ xác nhận",
    termCount: (count: number, employerCount: number) =>
      employerCount > 0
        ? `${count} mục (gồm ${employerCount} mục người sử dụng lao động nhập)`
        : `${count} mục`,
    remainingWarnings: (count: number) => `Còn ${count} mục cần kiểm tra.`,
    remainingWarningsHint: (names: string) =>
      `Màn hình của người lao động cũng sẽ hiện cùng lưu ý cho các mục: ${names}.`,
    submitNote:
      "Nếu sửa nội dung sau khi gửi, xác nhận của người lao động sẽ mất hiệu lực và cần xác nhận lại.",
    send: "Gửi",
    sending: "Đang gửi",
  },
} satisfies Record<UiLanguage, ContractDraftLabels>;
