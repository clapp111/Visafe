import type { UiLanguage } from "@/shared/i18n";

const ko = {
  title: "원본 계약서",
  loading: "원본 계약서를 불러오는 중입니다",
  loadError: "원본 계약서를 불러오지 못했습니다",
  readError: "원본 파일을 읽지 못했습니다",
  unsupported: "미리보기를 지원하지 않는 파일 형식입니다",
};

/** 원본 계약서 미리보기 문구 */
export const CONTRACT_FILE_VIEWER_LABELS = {
  ko,
  vi: {
    title: "Hợp đồng gốc",
    loading: "Đang tải hợp đồng gốc",
    loadError: "Không thể tải hợp đồng gốc",
    readError: "Không thể đọc tệp gốc",
    unsupported: "Định dạng tệp này không hỗ trợ xem trước",
  },
} satisfies Record<UiLanguage, typeof ko>;
