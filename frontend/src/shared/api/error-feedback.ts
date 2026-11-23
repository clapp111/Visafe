import type { UiLanguage } from "@/shared/i18n";

export type ApiErrorAction =
  "retry" | "login" | "edit" | "refresh" | "back" | "wait";

type Feedback = Record<UiLanguage, string> & { action: ApiErrorAction };

const loginRequired: Feedback = {
  ko: "다시 로그인해 주세요.",
  vi: "Vui lòng đăng nhập lại.",
  action: "login",
};

const invalidInput: Feedback = {
  ko: "입력한 정보를 다시 확인해 주세요.",
  vi: "Hãy kiểm tra lại thông tin đã nhập.",
  action: "edit",
};

const retryLater: Feedback = {
  ko: "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.",
  vi: "Không thể xử lý yêu cầu. Vui lòng thử lại sau.",
  action: "retry",
};

const feedbackByCode: Record<string, Feedback> = {
  AUTH_UNAUTHORIZED: loginRequired,
  JWT_INVALID: loginRequired,
  JWT_EXPIRED: loginRequired,
  JWT_MALFORMED: loginRequired,
  JWT_UNSUPPORTED: loginRequired,
  JWT_REFRESH_MISSING: loginRequired,
  JWT_REFRESH_NOT_MATCH: loginRequired,
  JWT_REFRESH_NOT_FOUND: loginRequired,

  COMMON_INVALID_REQUEST: invalidInput,
  COMMON_INVALID_REQUEST_TYPE: invalidInput,
  COMMON_INVALID_PARAMETER: invalidInput,
  COMMON_INTERNAL_SERVER_ERROR: retryLater,

  AUTH_LOGIN_FAILED: {
    ko: "이메일 또는 비밀번호가 올바르지 않습니다.",
    vi: "Email hoặc mật khẩu không chính xác.",
    action: "edit",
  },
  AUTH_FORBIDDEN: {
    ko: "이 작업을 수행할 권한이 없습니다.",
    vi: "Bạn không có quyền thực hiện thao tác này.",
    action: "back",
  },
  COMMON_HANDLER_NOT_FOUND: {
    ko: "요청한 화면이나 기능을 찾을 수 없습니다.",
    vi: "Không tìm thấy trang hoặc chức năng được yêu cầu.",
    action: "back",
  },
  COMMON_METHOD_NOT_ALLOWED: {
    ko: "이 작업을 수행할 수 없습니다. 화면을 새로고침해 주세요.",
    vi: "Không thể thực hiện thao tác này. Hãy tải lại trang.",
    action: "refresh",
  },
  COMMON_UNSUPPORTED_MEDIA_TYPE: {
    ko: "지원하는 형식으로 다시 입력해 주세요.",
    vi: "Hãy nhập lại theo định dạng được hỗ trợ.",
    action: "edit",
  },
  MEMBER_NOT_FOUND: {
    ko: "회원을 찾을 수 없습니다.",
    vi: "Không tìm thấy tài khoản.",
    action: "back",
  },
  CONTRACT_NOT_FOUND: {
    ko: "계약을 찾을 수 없습니다.",
    vi: "Không tìm thấy hợp đồng.",
    action: "back",
  },
  CONTRACT_STAGE_CONFLICT: {
    ko: "계약 단계가 바뀌었을 수 있습니다. 현재 상태를 다시 확인해 주세요.",
    vi: "Giai đoạn của hợp đồng có thể đã thay đổi. Hãy kiểm tra lại trạng thái hiện tại.",
    action: "refresh",
  },
  WORKER_NOT_FOUND: {
    ko: "근로자 계정을 찾을 수 없습니다. 입력한 정보를 확인해 주세요.",
    vi: "Không tìm thấy tài khoản người lao động. Hãy kiểm tra thông tin đã nhập.",
    action: "edit",
  },
  PREVIOUS_CONTRACT_INVALID: {
    ko: "같은 고용주와 근로자의 확정된 계약만 대체할 수 있습니다.",
    vi: "Chỉ có thể thay thế hợp đồng đã xác nhận giữa cùng người sử dụng lao động và người lao động.",
    action: "edit",
  },
  FILE_TOO_LARGE: {
    ko: "10MB 이하의 파일을 선택해 주세요.",
    vi: "Hãy chọn tệp có dung lượng không quá 10 MB.",
    action: "edit",
  },
  FILE_UNSUPPORTED_TYPE: {
    ko: "PDF, JPG 또는 PNG 파일을 선택해 주세요.",
    vi: "Hãy chọn tệp PDF, JPG hoặc PNG.",
    action: "edit",
  },
  FILE_NOT_FOUND: {
    ko: "파일을 찾을 수 없습니다.",
    vi: "Không tìm thấy tệp.",
    action: "back",
  },
  TERM_KEY_UNKNOWN: {
    ko: "계약 항목을 찾을 수 없습니다. 최신 내용을 불러와 주세요.",
    vi: "Không tìm thấy mục trong hợp đồng. Hãy tải lại nội dung mới nhất.",
    action: "refresh",
  },
  TERM_VALUE_INVALID: {
    ko: "계약 항목에 입력한 값을 확인해 주세요.",
    vi: "Hãy kiểm tra giá trị đã nhập trong mục hợp đồng.",
    action: "edit",
  },
  TERMS_REQUIRED_MISSING: {
    ko: "비어 있는 필수 항목을 입력해 주세요.",
    vi: "Hãy điền các mục bắt buộc còn trống.",
    action: "edit",
  },
  CONFIRMATION_REQUIRED: {
    ko: "입력 내용이 원본과 같은지 확인한 뒤 체크해 주세요.",
    vi: "Hãy đối chiếu nội dung đã nhập với bản gốc rồi đánh dấu xác nhận.",
    action: "edit",
  },
  TRANSLATION_NOT_READY: {
    ko: "번역이 아직 완료되지 않았습니다. 잠시 후 상태를 확인해 주세요.",
    vi: "Bản dịch chưa hoàn tất. Vui lòng kiểm tra lại trạng thái sau.",
    action: "wait",
  },
  TRANSLATION_OUTDATED: {
    ko: "새 번역본이 나왔습니다. 최신 내용을 다시 확인해 주세요.",
    vi: "Đã có bản dịch mới. Hãy kiểm tra lại nội dung mới nhất.",
    action: "refresh",
  },
  READ_ALL_REQUIRED: {
    ko: "계약 내용을 모두 읽은 뒤 확인해 주세요.",
    vi: "Hãy đọc toàn bộ nội dung hợp đồng trước khi xác nhận.",
    action: "edit",
  },
  REPORT_ITEMS_REQUIRED: {
    ko: "원본과 다른 항목을 하나 이상 선택해 주세요.",
    vi: "Hãy chọn ít nhất một mục khác với bản gốc.",
    action: "edit",
  },
  TERMINATION_ALREADY_ACTIVE: {
    ko: "이미 진행 중인 종료 요청이 있습니다. 현재 상태를 확인해 주세요.",
    vi: "Đã có yêu cầu chấm dứt đang được xử lý. Hãy kiểm tra trạng thái hiện tại.",
    action: "refresh",
  },
  TERMINATION_DATE_INVALID: {
    ko: "종료일은 오늘부터 계약 종료일까지 선택할 수 있습니다.",
    vi: "Có thể chọn ngày chấm dứt từ hôm nay đến hết ngày kết thúc hợp đồng.",
    action: "edit",
  },
  TERMINATION_NOT_ALLOWED: {
    ko: "이 종료 요청에 응답하거나 취소할 권한이 없습니다.",
    vi: "Bạn không có quyền phản hồi hoặc hủy yêu cầu chấm dứt này.",
    action: "back",
  },
  TERMINATION_STATUS_CONFLICT: {
    ko: "이미 처리된 종료 요청입니다. 현재 상태를 확인해 주세요.",
    vi: "Yêu cầu chấm dứt này đã được xử lý. Hãy kiểm tra trạng thái hiện tại.",
    action: "refresh",
  },
  AUTHORITY_ALREADY_REVIEWED: {
    ko: "이미 검토한 회차입니다. 최신 내용을 확인해 주세요.",
    vi: "Phiên bản này đã được xem xét. Hãy kiểm tra nội dung mới nhất.",
    action: "refresh",
  },
  REJECT_REASON_REQUIRED: {
    ko: "반려 사유를 입력해 주세요.",
    vi: "Hãy nhập lý do từ chối.",
    action: "edit",
  },
  NOTIFICATION_NOT_FOUND: {
    ko: "알림을 찾을 수 없습니다. 알림 목록을 다시 불러와 주세요.",
    vi: "Không tìm thấy thông báo. Hãy tải lại danh sách thông báo.",
    action: "refresh",
  },
  LOOKUP_INVALID_FORMAT: {
    ko: "확인번호 형식을 확인해 주세요.",
    vi: "Hãy kiểm tra định dạng của mã xác nhận.",
    action: "edit",
  },
  LOOKUP_NOT_FOUND: {
    ko: "확인번호에 해당하는 계약이 없습니다. 번호를 다시 확인해 주세요.",
    vi: "Không tìm thấy hợp đồng theo mã xác nhận này. Hãy kiểm tra lại mã.",
    action: "edit",
  },
  FILE_HASH_INVALID_FORMAT: {
    ko: "원본 확인 정보를 확인하고 파일을 다시 선택해 주세요.",
    vi: "Hãy kiểm tra thông tin xác minh bản gốc và chọn lại tệp.",
    action: "edit",
  },
};

export function getApiErrorFeedback(
  errorCode: string | undefined,
  language: UiLanguage,
) {
  const feedback =
    errorCode && Object.hasOwn(feedbackByCode, errorCode)
      ? feedbackByCode[errorCode]
      : retryLater;

  return { message: feedback[language], action: feedback.action };
}
