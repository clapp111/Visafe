import type { UiLanguage } from "@/shared/i18n";

const ko = {
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
  invalidCredentials: "이메일 또는 비밀번호가 올바르지 않습니다.",
  loginUnavailable: "로그인에 실패했습니다. 다시 시도해 주세요.",
  guestHint: "계약이 유효한지만 확인하려면 로그인하지 않아도 됩니다.",
  guestLink: "확인번호로 계약 조회",
};

export type LoginLabels = typeof ko;

/** 로그인 화면 문구 */
export const LOGIN_LABELS = {
  ko,
  vi: {
    breadcrumb: "Vị trí hiện tại",
    home: "Trang chủ",
    title: "Đăng nhập",
    description:
      "Người sử dụng lao động, người lao động và cơ quan giám sát đều đăng nhập tại đây. Sau khi đăng nhập, bạn sẽ được chuyển đến màn hình đầu tiên theo vai trò.",
    email: "Email",
    emailPlaceholder: "Ví dụ: name@example.com",
    password: "Mật khẩu",
    showPassword: "Hiện mật khẩu",
    hidePassword: "Ẩn mật khẩu",
    submit: "Đăng nhập",
    submitting: "Đang đăng nhập",
    emailRequired: "Vui lòng nhập email.",
    emailInvalid: "Email không đúng định dạng.",
    passwordRequired: "Vui lòng nhập mật khẩu.",
    failedHint: "Hãy kiểm tra lại thông tin đã nhập.",
    invalidCredentials: "Email hoặc mật khẩu không chính xác.",
    loginUnavailable: "Đăng nhập không thành công. Vui lòng thử lại.",
    guestHint:
      "Nếu chỉ cần kiểm tra hợp đồng còn hiệu lực, bạn không cần đăng nhập.",
    guestLink: "Tra cứu hợp đồng bằng mã xác nhận",
  },
} satisfies Record<UiLanguage, LoginLabels>;
