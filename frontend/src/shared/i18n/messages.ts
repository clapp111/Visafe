import type { UiLanguage } from "./language";

const ko = {
  language: "언어",
  switchLanguage: "Tiếng Việt",
  lookupContract: "계약 조회",
  myContracts: "내 계약",
  logout: "로그아웃",
  languageChangeFailed: "화면 언어를 변경하지 못했습니다.",
};

const vi = {
  language: "Ngôn ngữ",
  switchLanguage: "한국어",
  lookupContract: "Tra cứu hợp đồng",
  myContracts: "Hợp đồng của tôi",
  logout: "Đăng xuất",
  languageChangeFailed: "Không thể thay đổi ngôn ngữ hiển thị.",
};

export const messages = { ko, vi } satisfies Record<UiLanguage, typeof ko>;
