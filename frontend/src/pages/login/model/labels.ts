/** 로그인 화면 문구. 화면 언어(#54)가 붙으면 언어별 값으로 바꾼다. */
export const LOGIN_LABELS = {
  breadcrumb: "현재 위치",
  home: "홈",
  title: "로그인",
  description:
    "고용주·근로자·감독 기관 모두 이 화면에서 로그인합니다. 로그인하면 역할에 맞는 첫 화면으로 이동합니다.",
  email: "이메일",
  emailPlaceholder: "예: name@example.com",
  password: "비밀번호",
  showPassword: "비밀번호 보기",
  hidePassword: "비밀번호 숨기기",
  submit: "로그인",
  submitting: "로그인하는 중",
  emailRequired: "이메일을 입력해 주세요.",
  emailInvalid: "이메일 형식이 올바르지 않습니다.",
  passwordRequired: "비밀번호를 입력해 주세요.",
  failedHint: "입력한 내용을 다시 확인해 주세요.",
  guestHint: "계약이 유효한지만 확인하려면 로그인하지 않아도 됩니다.",
  guestLink: "확인번호로 계약 조회",
};

export type LoginLabels = typeof LOGIN_LABELS;
