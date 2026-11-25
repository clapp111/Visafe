import type { TermValueLabels } from "@/entities/contract";
import type { components } from "@/shared/api";
import type { UiLanguage } from "@/shared/i18n";

type Stage = NonNullable<
  components["schemas"]["AdminContractDetailResponse"]["stage"]
>;

const ko = {
  breadcrumb: "현재 위치",
  home: "홈",
  list: "확인 대기 목록",
  title: "계약 검토",
  description: "원본 계약서와 확정 항목을 비교하고, 확정하거나 반려하세요.",
  loading: "계약을 불러오는 중",
  loadError: "계약을 불러오지 못했습니다",

  summary: {
    contractNo: "계약 번호",
    employer: "고용주",
    worker: "근로자",
    stage: "상태",
  },
  stages: {
    UPLOADED: "고용주 등록 중",
    REVIEWING: "고용주 검토 중",
    TRANSLATING: "번역 중",
    AWAITING_WORKER: "근로자 확인 대기",
    RETURNED: "고용주에게 돌아감",
    AUTHORITY_REVIEW: "기관 검토 대기",
    RECORDING: "블록체인 기록 중",
    DONE: "확정",
    ENDED: "종료",
  } satisfies Record<Stage, string>,

  viewerHint: "오른쪽 항목과 비교해 보세요.",
  viewerPreview: (page: number) => `원본 계약서 ${page}쪽 미리보기`,
  viewerDownload: "원본 내려받기",

  attentionTitle: (count: number) => `검토할 항목이 ${count}개 있습니다`,
  warningTag: "확인 필요",
  employerTag: "고용주 입력",
  employerReason: "AI 값과 다름",
  attentionHint:
    "경고가 남아 있어도 확정할 수 있습니다. 경고는 확정 후에도 이력에 그대로 남습니다.",

  fieldsTitle: "확정 항목",
  fieldsCaption: "고용주가 제출하고 근로자가 확인한 계약 항목",
  columns: { label: "항목", value: "최종 값", note: "비고" },
  aiValue: (value: string) => `AI가 읽은 값: ${value}`,
  aiUnread: "읽지 못함",
  showSource: (pageNo: number) => `원본 ${pageNo}쪽`,
  noValue: "없음",
  units: {
    won: (amount: string) => `${amount}원`,
    minutes: (minutes: number) => `${minutes}분`,
    daysPerWeek: (days: number) => `주 ${days}일`,
    payDay: (day: number) => `매월 ${day}일`,
  },

  editLogsTitle: "항목 수정 이력",
  editLogsEmpty: "고용주가 고친 항목이 없습니다.",
  editLog: (before: string, after: string) => `${before} → ${after}`,
  historyTitle: "진행 이력",

  actionTitle: "이 계약을 확정할까요?",
  actionDescription:
    "확정하면 내용을 바꿀 수 없습니다. 조건이 바뀌면 새 계약으로 등록해야 합니다. 반려하면 사유와 함께 고용주에게 돌아갑니다.",
  reject: "반려",
  approve: "확정",
  reviewedTitle: "지금은 확정하거나 반려할 수 없는 계약입니다",
  reviewedDescription:
    "기관 검토 단계가 아닙니다. 진행 상황은 아래 진행 이력에서 확인하세요.",

  approveTitle: "이 계약을 확정할까요?",
  remainingWarnings: "남은 경고",
  noWarnings: "없음",
  warningsValue: (names: string) => `${names} · 이력에 남습니다`,
  approveNotes: [
    "확정하면 내용을 바꿀 수 없습니다.",
    "원본·확정 항목·번역본의 해시가 블록체인(Sepolia 테스트넷)에 기록됩니다.",
    "기록이 끝나면 확인번호가 발급되어 고용주와 근로자에게 전달됩니다.",
  ],
  cancel: "취소",
  approving: "확정하는 중",

  rejectTitle: "반려 사유를 입력하세요",
  rejectDescription:
    "사유는 고용주에게 전달됩니다. 계약은 고용주의 '추출 항목 검토' 단계로 돌아가고, 고용주가 수정해 다시 제출하면 근로자가 다시 확인합니다.",
  rejectReason: "반려 사유",
  rejectReasonHint: "반려 사유를 반드시 입력해야 합니다.",
  rejectReasonRequired: "반려 사유를 입력해 주세요.",
  rejectSubmit: "반려하기",
  rejecting: "반려하는 중",
} satisfies TermValueLabels & Record<string, unknown>;

export type AdminContractReviewLabels = typeof ko;

/** 감독 기관 계약 검토 문구 */
export const ADMIN_CONTRACT_REVIEW_LABELS = {
  ko,
  vi: {
    breadcrumb: "Vị trí hiện tại",
    home: "Trang chủ",
    list: "Danh sách chờ xác nhận",
    title: "Kiểm tra hợp đồng",
    description:
      "Hãy so sánh hợp đồng gốc với các mục sẽ xác nhận, rồi xác nhận hoặc trả lại.",
    loading: "Đang tải hợp đồng",
    loadError: "Không thể tải hợp đồng",

    summary: {
      contractNo: "Số hợp đồng",
      employer: "Người sử dụng lao động",
      worker: "Người lao động",
      stage: "Trạng thái",
    },
    stages: {
      UPLOADED: "Người sử dụng lao động đang đăng ký",
      REVIEWING: "Người sử dụng lao động đang kiểm tra",
      TRANSLATING: "Đang dịch",
      AWAITING_WORKER: "Chờ người lao động xác nhận",
      RETURNED: "Đã trả lại người sử dụng lao động",
      AUTHORITY_REVIEW: "Chờ cơ quan kiểm tra",
      RECORDING: "Đang ghi lên blockchain",
      DONE: "Đã xác nhận",
      ENDED: "Đã chấm dứt",
    },

    viewerHint: "Hãy so sánh với các mục bên phải.",
    viewerPreview: (page: number) => `Xem trước trang ${page} của hợp đồng gốc`,
    viewerDownload: "Tải xuống bản gốc",

    attentionTitle: (count: number) => `Có ${count} mục cần kiểm tra`,
    warningTag: "Cần kiểm tra",
    employerTag: "Người sử dụng lao động nhập",
    employerReason: "Khác với giá trị AI",
    attentionHint:
      "Vẫn có thể xác nhận dù còn cảnh báo. Cảnh báo vẫn được lưu trong lịch sử sau khi xác nhận.",

    fieldsTitle: "Các mục sẽ xác nhận",
    fieldsCaption:
      "Các mục hợp đồng do người sử dụng lao động gửi và người lao động đã xác nhận",
    columns: { label: "Mục", value: "Giá trị cuối cùng", note: "Ghi chú" },
    aiValue: (value: string) => `Giá trị AI đọc được: ${value}`,
    aiUnread: "Không đọc được",
    showSource: (pageNo: number) => `Bản gốc trang ${pageNo}`,
    noValue: "Không có",
    units: {
      won: (amount: string) => `${amount} won`,
      minutes: (minutes: number) => `${minutes} phút`,
      daysPerWeek: (days: number) => `${days} ngày/tuần`,
      payDay: (day: number) => `ngày ${day} hằng tháng`,
    },

    editLogsTitle: "Lịch sử sửa các mục",
    editLogsEmpty: "Người sử dụng lao động không sửa mục nào.",
    editLog: (before: string, after: string) => `${before} → ${after}`,
    historyTitle: "Lịch sử xử lý",

    actionTitle: "Xác nhận hợp đồng này?",
    actionDescription:
      "Sau khi xác nhận, không thể thay đổi nội dung. Nếu điều kiện thay đổi, phải đăng ký hợp đồng mới. Nếu trả lại, hợp đồng sẽ quay về người sử dụng lao động kèm lý do.",
    reject: "Trả lại",
    approve: "Xác nhận",
    reviewedTitle: "Hiện không thể xác nhận hoặc trả lại hợp đồng này",
    reviewedDescription:
      "Hợp đồng không ở bước cơ quan kiểm tra. Hãy xem tiến trình trong lịch sử xử lý bên dưới.",

    approveTitle: "Xác nhận hợp đồng này?",
    remainingWarnings: "Cảnh báo còn lại",
    noWarnings: "Không có",
    warningsValue: (names: string) => `${names} · được lưu trong lịch sử`,
    approveNotes: [
      "Sau khi xác nhận, không thể thay đổi nội dung.",
      "Mã băm của bản gốc, các mục đã xác nhận và bản dịch được ghi lên blockchain (Sepolia testnet).",
      "Khi ghi xong, mã xác nhận được cấp và gửi cho người sử dụng lao động và người lao động.",
    ],
    cancel: "Hủy",
    approving: "Đang xác nhận",

    rejectTitle: "Nhập lý do trả lại",
    rejectDescription:
      "Lý do sẽ được gửi cho người sử dụng lao động. Hợp đồng quay về bước kiểm tra các mục đã trích xuất của người sử dụng lao động; khi người sử dụng lao động sửa và gửi lại, người lao động sẽ xác nhận lại.",
    rejectReason: "Lý do trả lại",
    rejectReasonHint: "Bắt buộc phải nhập lý do trả lại.",
    rejectReasonRequired: "Vui lòng nhập lý do trả lại.",
    rejectSubmit: "Trả lại",
    rejecting: "Đang trả lại",
  },
} satisfies Record<UiLanguage, AdminContractReviewLabels>;
