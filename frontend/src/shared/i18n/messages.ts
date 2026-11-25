import type { UiLanguage } from "./language";

const ko = {
  language: "언어",
  switchLanguage: "Tiếng Việt",
  switchLanguageLabel: "화면 언어를 베트남어로 변경",
  lookupContract: "계약 조회",
  myContracts: "내 계약",
  logout: "로그아웃",
  logoutFailed: "로그아웃하지 못했습니다. 다시 시도해 주세요.",
  languageChangeFailed: "화면 언어를 변경하지 못했습니다.",
  close: "닫기",
  required: "필수",
  loading: "불러오는 중입니다",
  retry: "다시 시도",
  dropFile: "파일을 이곳에 끌어다 놓거나",
  selectFile: "파일 선택",
  previousPage: "이전 쪽",
  nextPage: "다음 쪽",
  pageStatus: (page: number, pageCount: number) => `${page} / ${pageCount}쪽`,
  pageCount: (pageCount: number) => `${pageCount}쪽`,
  pagePreview: (page: number) => `${page}쪽 미리보기`,
  download: "내려받기",
};

const vi = {
  language: "Ngôn ngữ",
  switchLanguage: "한국어",
  switchLanguageLabel: "Đổi ngôn ngữ hiển thị sang tiếng Hàn",
  lookupContract: "Tra cứu hợp đồng",
  myContracts: "Hợp đồng của tôi",
  logout: "Đăng xuất",
  logoutFailed: "Không thể đăng xuất. Vui lòng thử lại.",
  languageChangeFailed: "Không thể thay đổi ngôn ngữ hiển thị.",
  close: "Đóng",
  required: "Bắt buộc",
  loading: "Đang tải",
  retry: "Thử lại",
  dropFile: "Kéo tệp thả vào đây hoặc",
  selectFile: "Chọn tệp",
  previousPage: "Trang trước",
  nextPage: "Trang sau",
  pageStatus: (page: number, pageCount: number) =>
    `Trang ${page} / ${pageCount}`,
  pageCount: (pageCount: number) => `${pageCount} trang`,
  pagePreview: (page: number) => `Xem trước trang ${page}`,
  download: "Tải xuống",
};

export const messages = { ko, vi } satisfies Record<UiLanguage, typeof ko>;
