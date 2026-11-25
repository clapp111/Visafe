import type { components } from "@/shared/api";
import type { UiLanguage } from "@/shared/i18n";

type Schemas = components["schemas"];

const ko = {
  breadcrumb: "현재 위치",
  home: "홈",
  lookup: "계약 조회",
  title: "조회 결과",
  lookupForm: {
    label: "확인번호",
    submit: "다시 조회",
    required: "확인번호를 입력해 주세요.",
  },
  notFound: "입력한 확인번호로 등록된 계약이 없습니다.",
  invalidFormat: "확인번호 형식이 올바르지 않습니다.",

  confirmationNo: "확인번호",
  employer: "고용주",
  worker: "근로자",
  masked: "(일부 가림)",
  period: "계약기간",
  confirmedAt: "확정 일시",
  terminatedOn: "종료일",
  status: {
    confirmed: {
      badge: "확정",
      title: "현재 유효한 근로계약입니다",
    },
    terminated: {
      badge: "종료",
      title: "종료된 근로계약입니다",
      description:
        "더 이상 유효하지 않은 계약입니다. 확인 기록은 이력으로 남아 있습니다.",
    },
  },
  /** 확정 계약 설명. 확정 기록의 무결성(integrity.status)에 따라 바뀐다 */
  integrity: {
    VERIFIED: "근로자가 계약 내용을 확인했고, 확정 이후 바뀐 기록이 없습니다.",
    MISMATCH:
      "확정 기록이 블록체인에 기록된 값과 다릅니다. 계약서를 준 고용주에게 확인해 주세요.",
    PENDING:
      "근로자가 계약 내용을 확인했고, 블록체인 기록을 기다리는 중입니다.",
    UNAVAILABLE:
      "근로자가 계약 내용을 확인했습니다. 지금은 블록체인 기록과 대조할 수 없으니 잠시 후 다시 조회해 주세요.",
  } satisfies Record<
    NonNullable<Schemas["LookupResponseIntegrity"]["status"]>,
    string
  >,

  terminationReason: "종료 사유",
  terminationReasons: {
    EXPIRED: "계약 기간 만료",
    MUTUAL: "근로자·고용주 합의 종료",
    REPLACED: "새 계약으로 대체",
  } satisfies Record<
    NonNullable<Schemas["LookupResponseTermination"]["reasonType"]>,
    string
  >,
  terminationEndDate: (date: string) => `종료일은 ${date}입니다.`,
  terminationNewNumber:
    "근로조건이 바뀌어 새 계약을 맺었다면, 새로 발급된 확인번호로 조회하세요.",

  keyTermsTitle: "핵심 근로조건",
  keyTermsHint:
    "가지고 계신 계약서와 같은지 비교해 보세요. 임금을 포함한 전체 항목은 계약 당사자만 '내 계약'에서 볼 수 있습니다.",
  workHours: "근로시간",
  workDays: (days: number) => `주 ${days}일`,
  weeklySummary: (hours: number, breakMinutes: number) =>
    `주 ${hours}시간 · 휴게 ${breakMinutes}분`,
  noEndDate: "기간의 정함 없음",
  aboutYears: (years: number) => `약 ${years}년`,
  aboutMonths: (months: number) => `약 ${months}개월`,

  fileCheckTitle: "가지고 있는 계약서와 대조하기",
  fileCheckHint:
    "파일을 올리면 내용에서 해시(고유값)를 계산해 블록체인에 기록된 값과 비교합니다. 파일은 저장되지 않습니다.",
  fileCheckInput: "대조할 계약서 파일",
  noFile: "선택한 파일 없음",
  selectFile: "파일 선택",
  changeFile: "다른 파일 선택",
  check: "대조하기",
  checking: "대조하는 중",
  fileReadError: "파일을 읽지 못했습니다. 파일을 다시 선택해 주세요.",
  matchTitle: "일치 — 등록된 원본과 같은 파일입니다",
  matchDescription: "이 파일은 확정 이후 바뀌지 않았습니다.",
  mismatchTitle: "불일치 — 등록된 원본과 다른 파일입니다",
  mismatchDescription: "파일이 확정 이후 바뀌었거나, 다른 파일일 수 있습니다.",
  uploadedFileHash: "올린 파일 해시",
  recordedFileHash: "기록된 해시",
  mismatchTips: [
    "같은 계약서라도 다시 스캔하거나 사진을 찍은 파일은 값이 달라집니다. 등록할 때 올린 바로 그 파일이어야 합니다.",
    "등록된 파일과 같아야 하는데 다르다면 계약서가 바뀌었을 수 있습니다. 고용노동부 상담센터(국번 없이 1350)에 문의하세요.",
  ],

  chainTitle: "블록체인 기록",
  historyTitle: "확인 이력",

  notFoundTitle: "확인번호를 다시 확인해 주세요",
  notFoundTips: {
    format: [
      "확인번호는 ",
      "VS-",
      "로 시작하는 12자리입니다. 숫자 0과 영문 O, 숫자 1과 영문 I는 헷갈리지 않도록 쓰지 않습니다.",
    ],
    confirmed: [
      "확인번호는 ",
      "계약이 확정된 뒤",
      "에 발급됩니다. 아직 근로자 확인 전인 계약은 조회되지 않습니다.",
    ],
    renewed: "근로조건이 바뀌어 새 계약을 맺었다면 확인번호도 새로 발급됩니다.",
  },
  notFoundHint:
    "계속 찾을 수 없다면 계약서를 준 고용주에게 확인번호를 다시 받아 보세요.",
};

export type LookupResultLabels = typeof ko;

/** 조회 결과 화면 문구 */
export const LOOKUP_RESULT_LABELS = {
  ko,
  vi: {
    breadcrumb: "Vị trí hiện tại",
    home: "Trang chủ",
    lookup: "Tra cứu hợp đồng",
    title: "Kết quả tra cứu",
    lookupForm: {
      label: "Mã xác nhận",
      submit: "Tra cứu lại",
      required: "Vui lòng nhập mã xác nhận.",
    },
    notFound: "Không có hợp đồng nào được đăng ký với mã xác nhận đã nhập.",
    invalidFormat: "Mã xác nhận không đúng định dạng.",

    confirmationNo: "Mã xác nhận",
    employer: "Người sử dụng lao động",
    worker: "Người lao động",
    masked: "(đã che một phần)",
    period: "Thời hạn hợp đồng",
    confirmedAt: "Thời điểm xác nhận",
    terminatedOn: "Ngày chấm dứt",
    status: {
      confirmed: {
        badge: "Đã xác nhận",
        title: "Hợp đồng lao động đang có hiệu lực",
      },
      terminated: {
        badge: "Đã chấm dứt",
        title: "Hợp đồng lao động đã chấm dứt",
        description:
          "Hợp đồng không còn hiệu lực. Lịch sử xác nhận vẫn được lưu lại.",
      },
    },
    integrity: {
      VERIFIED:
        "Người lao động đã xác nhận nội dung hợp đồng và không có thay đổi nào sau khi xác nhận.",
      MISMATCH:
        "Bản ghi xác nhận khác với giá trị đã ghi trên blockchain. Hãy hỏi lại người sử dụng lao động đã đưa hợp đồng cho bạn.",
      PENDING:
        "Người lao động đã xác nhận nội dung hợp đồng, đang chờ ghi lên blockchain.",
      UNAVAILABLE:
        "Người lao động đã xác nhận nội dung hợp đồng. Hiện chưa thể đối chiếu với blockchain, vui lòng tra cứu lại sau.",
    },

    terminationReason: "Lý do chấm dứt",
    terminationReasons: {
      EXPIRED: "Hết thời hạn hợp đồng",
      MUTUAL: "Người lao động và người sử dụng lao động thỏa thuận chấm dứt",
      REPLACED: "Được thay bằng hợp đồng mới",
    },
    terminationEndDate: (date: string) => `Ngày chấm dứt là ${date}.`,
    terminationNewNumber:
      "Nếu đã ký hợp đồng mới vì điều kiện lao động thay đổi, hãy tra cứu bằng mã xác nhận mới được cấp.",

    keyTermsTitle: "Điều kiện lao động chính",
    keyTermsHint:
      "Hãy so sánh với hợp đồng bạn đang giữ. Toàn bộ các mục, kể cả tiền lương, chỉ các bên trong hợp đồng mới xem được tại mục Hợp đồng của tôi.",
    workHours: "Giờ làm việc",
    workDays: (days: number) => `${days} ngày/tuần`,
    weeklySummary: (hours: number, breakMinutes: number) =>
      `${hours} giờ/tuần · nghỉ giải lao ${breakMinutes} phút`,
    noEndDate: "Không xác định thời hạn",
    aboutYears: (years: number) => `khoảng ${years} năm`,
    aboutMonths: (months: number) => `khoảng ${months} tháng`,

    fileCheckTitle: "Đối chiếu với hợp đồng bạn đang giữ",
    fileCheckHint:
      "Khi bạn tải tệp lên, hệ thống tính mã băm (giá trị riêng) từ nội dung và so sánh với giá trị đã ghi trên blockchain. Tệp không được lưu lại.",
    fileCheckInput: "Tệp hợp đồng cần đối chiếu",
    noFile: "Chưa chọn tệp",
    selectFile: "Chọn tệp",
    changeFile: "Chọn tệp khác",
    check: "Đối chiếu",
    checking: "Đang đối chiếu",
    fileReadError: "Không đọc được tệp. Vui lòng chọn lại tệp.",
    matchTitle: "Trùng khớp — Đây là cùng tệp với bản gốc đã đăng ký",
    matchDescription: "Tệp này không thay đổi kể từ khi xác nhận.",
    mismatchTitle: "Không khớp — Tệp khác với bản gốc đã đăng ký",
    mismatchDescription:
      "Tệp có thể đã bị thay đổi sau khi xác nhận, hoặc là một tệp khác.",
    uploadedFileHash: "Mã băm của tệp đã tải lên",
    recordedFileHash: "Mã băm đã ghi",
    mismatchTips: [
      "Dù cùng một hợp đồng, tệp được quét lại hoặc chụp ảnh lại sẽ có giá trị khác. Phải là đúng tệp đã tải lên khi đăng ký.",
      "Nếu tệp lẽ ra phải giống bản đã đăng ký nhưng lại khác, hợp đồng có thể đã bị thay đổi. Hãy liên hệ Trung tâm tư vấn của Bộ Việc làm và Lao động Hàn Quốc (gọi 1350, không cần mã vùng).",
    ],

    chainTitle: "Bản ghi blockchain",
    historyTitle: "Lịch sử xác nhận",

    notFoundTitle: "Vui lòng kiểm tra lại mã xác nhận",
    notFoundTips: {
      format: [
        "Mã xác nhận gồm 12 ký tự, bắt đầu bằng ",
        "VS-",
        ". Mã không dùng số 0 và chữ O, số 1 và chữ I để tránh nhầm lẫn.",
      ],
      confirmed: [
        "Mã xác nhận được cấp ",
        "sau khi hợp đồng được xác nhận",
        ". Hợp đồng chưa được người lao động xác nhận sẽ không tra cứu được.",
      ],
      renewed:
        "Nếu đã ký hợp đồng mới vì điều kiện lao động thay đổi, mã xác nhận cũng được cấp mới.",
    },
    notFoundHint:
      "Nếu vẫn không tìm thấy, hãy xin lại mã xác nhận từ người sử dụng lao động đã đưa hợp đồng cho bạn.",
  },
} satisfies Record<UiLanguage, LookupResultLabels>;
