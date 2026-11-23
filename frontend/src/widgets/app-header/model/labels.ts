import type { components } from "@/shared/api";

export type MemberRole = NonNullable<
  components["schemas"]["MemberResponse"]["role"]
>;

/** 헤더 문구. 화면 언어(#54)가 붙으면 쓰는 쪽에서 언어별 값을 넘긴다. */
export const DEFAULT_HEADER_LABELS = {
  skipToContent: "본문 바로가기",
  home: "Visafe 홈",
  tagline: "근로계약 확인 기록 · 변경 감지",
  mainNav: "주 메뉴",
  lookup: "계약 조회",
  contracts: "내 계약",
  contractNew: "계약 등록",
  adminPending: "확인 대기 목록",
  adminApproved: "확정",
  adminRejected: "반려",
  adminTerminated: "종료",
  login: "로그인",
  logout: "로그아웃",
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
