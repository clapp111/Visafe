import type { UiLanguage } from "@/shared/i18n";

const ko = {
  label: "등록 단계",
  current: "현재 단계",
  next: "다음 단계",
  done: "완료",
  items: ["근로계약서 등록", "계약 내용 검토 · 수정"],
};

export type RegisterStepsLabels = typeof ko;

/** 계약 등록 단계 문구 */
export const REGISTER_STEPS_LABELS = {
  ko,
  vi: {
    label: "Các bước đăng ký",
    current: "Bước hiện tại",
    next: "Bước tiếp theo",
    done: "Hoàn tất",
    items: ["Đăng ký hợp đồng lao động", "Kiểm tra · Sửa nội dung hợp đồng"],
  },
} satisfies Record<UiLanguage, RegisterStepsLabels>;
