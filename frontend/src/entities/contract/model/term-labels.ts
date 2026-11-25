import type { UiLanguage } from "@/shared/i18n";

/** 항목 이름 (베트남어). 서버(TermField)는 한국어 이름만 주므로 키로 바꾼다. */
const TERM_LABELS_VI: Record<string, string> = {
  employerName: "Tên doanh nghiệp",
  representativeName: "Người đại diện",
  employerAddress: "Địa chỉ nơi làm việc của doanh nghiệp",
  workerName: "Họ tên người lao động",
  workerNationality: "Quốc tịch",
  startDate: "Ngày bắt đầu hợp đồng",
  endDate: "Ngày kết thúc hợp đồng",
  workPlace: "Nơi làm việc",
  jobDescription: "Nội dung công việc",
  workStartTime: "Giờ bắt đầu làm việc",
  workEndTime: "Giờ kết thúc làm việc",
  breakMinutes: "Thời gian nghỉ giải lao (phút)",
  workDaysPerWeek: "Số ngày làm việc mỗi tuần",
  workDaysText: "Ngày làm việc (nguyên văn)",
  weeklyHoliday: "Ngày nghỉ hằng tuần",
  monthlyBaseWage: "Lương cơ bản hằng tháng (won)",
  payDay: "Ngày trả lương",
  payMethod: "Cách trả lương",
  accommodation: "Cung cấp chỗ ăn ở",
};

/** 화면 언어에 맞는 항목 이름. 한국어는 서버가 준 이름을 그대로 쓴다. */
export function termLabel(
  key: string | undefined,
  serverLabel: string | null | undefined,
  language: UiLanguage,
) {
  const translated = language === "vi" && key ? TERM_LABELS_VI[key] : undefined;
  return translated ?? serverLabel ?? key ?? "";
}
