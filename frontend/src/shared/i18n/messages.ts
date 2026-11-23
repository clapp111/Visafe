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
};

export const messages = { ko, vi } satisfies Record<UiLanguage, typeof ko>;
