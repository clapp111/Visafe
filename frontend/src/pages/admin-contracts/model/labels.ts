import type { UiLanguage } from "@/shared/i18n";

export type AdminView = "PENDING" | "APPROVED" | "REJECTED" | "TERMINATED";

const ko = {
  breadcrumb: "현재 위치",
  home: "홈",
  loading: "계약 목록을 불러오는 중",
  loadError: "계약 목록을 불러오지 못했습니다",

  titles: {
    PENDING: "확인 대기 목록",
    APPROVED: "확정한 계약",
    REJECTED: "반려한 계약",
    TERMINATED: "종료된 계약",
  } satisfies Record<AdminView, string>,
  descriptions: {
    PENDING:
      "근로자 확인까지 끝난 계약입니다. 내용을 검토한 뒤 확정하거나 반려하세요.",
    APPROVED: "기관이 확정한 계약입니다.",
    REJECTED: "기관이 반려해 고용주에게 돌려보낸 계약입니다.",
    TERMINATED: "기관이 확정한 뒤 종료된 계약입니다.",
  } satisfies Record<AdminView, string>,

  searchLabel: "계약 번호 · 고용주 · 근로자 검색",
  searchPlaceholder: "예: C-2026-04817",
  search: "검색",
  total: (count: number) => `전체 ${count}건`,
  sortOrder: "근로자 확인이 오래된 순",
  legendWarning: "확인 필요",
  legendWarningHint: "법 기준 등 경고가 남은 항목",
  legendEmployer: "고용주 입력",
  legendEmployerHint: "고용주가 직접 입력·수정한 항목",

  caption: (title: string) => `${title} 계약 목록`,
  columns: {
    contractNo: "계약 번호",
    employer: "고용주",
    worker: "근로자",
    language: "언어",
    workerConfirmedAt: "근로자 확인 일시",
    warningCount: "확인 필요",
    employerInputCount: "고용주 입력",
    action: "검토",
  },
  count: (count: number) => `${count}건`,
  review: "검토",
  view: "보기",
  languageNames: { vi: "베트남어" } as Record<string, string>,

  empty: {
    PENDING: "확인을 기다리는 계약이 없습니다",
    APPROVED: "확정한 계약이 없습니다",
    REJECTED: "반려한 계약이 없습니다",
    TERMINATED: "종료된 계약이 없습니다",
  } satisfies Record<AdminView, string>,
  emptySearch: "검색 결과가 없습니다",
  emptySearchHint: "계약 번호나 이름을 다시 확인해 주세요.",

  infoTitle: "감독 기관 확인이란?",
  info: [
    "확정하면 계약 내용의 해시가 블록체인(Sepolia 테스트넷)에 기록되고 확인번호가 발급됩니다.",
    "번역 내용은 이 단계에서 검토하지 않습니다. 번역은 근로자 확인과 '원본과 다릅니다' 신고로 검증합니다.",
  ],

  pagination: "목록 쪽 이동",
  previousPage: "이전",
  nextPage: "다음",
  pageStatus: (page: number, total: number) => `${page} / ${total}쪽`,
};

export type AdminContractsLabels = typeof ko;

/** 감독 기관 목록 문구 */
export const ADMIN_CONTRACTS_LABELS = {
  ko,
  vi: {
    breadcrumb: "Vị trí hiện tại",
    home: "Trang chủ",
    loading: "Đang tải danh sách hợp đồng",
    loadError: "Không thể tải danh sách hợp đồng",

    titles: {
      PENDING: "Danh sách chờ xác nhận",
      APPROVED: "Hợp đồng đã xác nhận",
      REJECTED: "Hợp đồng đã trả lại",
      TERMINATED: "Hợp đồng đã chấm dứt",
    },
    descriptions: {
      PENDING:
        "Các hợp đồng người lao động đã xác nhận xong. Hãy kiểm tra nội dung rồi xác nhận hoặc trả lại.",
      APPROVED: "Các hợp đồng cơ quan đã xác nhận.",
      REJECTED: "Các hợp đồng cơ quan đã trả lại cho người sử dụng lao động.",
      TERMINATED: "Các hợp đồng đã chấm dứt sau khi cơ quan xác nhận.",
    },

    searchLabel:
      "Tìm theo số hợp đồng · người sử dụng lao động · người lao động",
    searchPlaceholder: "Ví dụ: C-2026-04817",
    search: "Tìm",
    total: (count: number) => `Tổng ${count} hợp đồng`,
    sortOrder: "Xác nhận của người lao động cũ nhất trước",
    legendWarning: "Cần kiểm tra",
    legendWarningHint: "Mục còn cảnh báo như tiêu chuẩn pháp luật",
    legendEmployer: "Người sử dụng lao động nhập",
    legendEmployerHint: "Mục do người sử dụng lao động tự nhập hoặc sửa",

    caption: (title: string) => `Danh sách hợp đồng: ${title}`,
    columns: {
      contractNo: "Số hợp đồng",
      employer: "Người sử dụng lao động",
      worker: "Người lao động",
      language: "Ngôn ngữ",
      workerConfirmedAt: "Thời điểm người lao động xác nhận",
      warningCount: "Cần kiểm tra",
      employerInputCount: "Người sử dụng lao động nhập",
      action: "Kiểm tra",
    },
    count: (count: number) => `${count} mục`,
    review: "Kiểm tra",
    view: "Xem",
    languageNames: { vi: "Tiếng Việt" } as Record<string, string>,

    empty: {
      PENDING: "Không có hợp đồng nào đang chờ xác nhận",
      APPROVED: "Không có hợp đồng đã xác nhận",
      REJECTED: "Không có hợp đồng đã trả lại",
      TERMINATED: "Không có hợp đồng đã chấm dứt",
    },
    emptySearch: "Không có kết quả tìm kiếm",
    emptySearchHint: "Hãy kiểm tra lại số hợp đồng hoặc tên.",

    infoTitle: "Cơ quan giám sát xác nhận là gì?",
    info: [
      "Khi xác nhận, mã băm của nội dung hợp đồng được ghi lên blockchain (Sepolia testnet) và mã xác nhận được cấp.",
      "Bước này không kiểm tra nội dung bản dịch. Bản dịch được kiểm chứng qua xác nhận của người lao động và báo Khác với bản gốc.",
    ],

    pagination: "Chuyển trang danh sách",
    previousPage: "Trước",
    nextPage: "Sau",
    pageStatus: (page: number, total: number) => `Trang ${page} / ${total}`,
  },
} satisfies Record<UiLanguage, AdminContractsLabels>;
