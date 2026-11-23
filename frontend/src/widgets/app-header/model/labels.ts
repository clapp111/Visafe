import type { components } from "@/shared/api";
import { messages, type UiLanguage } from "@/shared/i18n";

export type MemberRole = NonNullable<
  components["schemas"]["MemberResponse"]["role"]
>;

export const DEFAULT_HEADER_LABELS = {
  skipToContent: "본문 바로가기",
  home: "Visafe 홈",
  tagline: "근로계약 확인 기록 · 변경 감지",
  mainNav: "주 메뉴",
  lookup: messages.ko.lookupContract,
  contracts: messages.ko.myContracts,
  contractNew: "계약 등록",
  adminPending: "확인 대기 목록",
  adminApproved: "확정",
  adminRejected: "반려",
  adminTerminated: "종료",
  login: "로그인",
  logout: messages.ko.logout,
  userMenu: "사용자 메뉴",
  /** 이름 뒤에 붙는 말 (예: "님"). 없으면 빈 문자열 */
  userSuffix: "님",
  roles: {
    EMPLOYER: "고용주",
    WORKER: "근로자",
    ADMIN: "감독 기관",
  } satisfies Record<MemberRole, string>,
};

export type AppHeaderLabels = typeof DEFAULT_HEADER_LABELS;

export const HEADER_LABELS = {
  ko: DEFAULT_HEADER_LABELS,
  vi: {
    skipToContent: "Chuyển đến nội dung chính",
    home: "Trang chủ Visafe",
    tagline: "Lưu xác nhận hợp đồng lao động · Phát hiện thay đổi",
    mainNav: "Menu chính",
    lookup: messages.vi.lookupContract,
    contracts: messages.vi.myContracts,
    contractNew: "Đăng ký hợp đồng",
    adminPending: "Danh sách chờ xác nhận",
    adminApproved: "Đã xác nhận",
    adminRejected: "Đã từ chối",
    adminTerminated: "Đã chấm dứt",
    login: "Đăng nhập",
    logout: messages.vi.logout,
    userMenu: "Menu tài khoản",
    userSuffix: "",
    roles: {
      EMPLOYER: "Người sử dụng lao động",
      WORKER: "Người lao động",
      ADMIN: "Cơ quan giám sát",
    },
  },
} satisfies Record<UiLanguage, AppHeaderLabels>;
