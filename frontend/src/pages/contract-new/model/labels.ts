/** 계약 등록 1단계 문구. 화면 언어(#54)가 붙으면 언어별 값으로 바꾼다. */
export const CONTRACT_NEW_LABELS = {
  breadcrumb: "현재 위치",
  home: "홈",
  title: "근로계약서 등록",
  description: [
    "이미 서명한 근로계약서 원본을 올리면 AI가 임금·근로시간·계약기간 등 핵심 항목을 읽어 옵니다.",
    "읽어 온 내용을 검토해 제출하면 베트남어 번역본이 만들어져 근로자에게 확인 요청이 전달됩니다.",
  ],
  steps: {
    label: "등록 단계",
    current: "현재 단계",
    next: "다음 단계",
    done: "완료",
    items: ["근로계약서 등록", "계약 내용 검토 • 수정"],
  },
  requiredNotice: "표시는 필수 입력 항목입니다.",
  required: "필수",

  fileTitle: "1. 근로계약서 원본",
  fileLabel: "원본 파일",
  fileHint:
    "근로자와 서명을 마친 계약서 1부를 올려 주세요. PDF, JPG, PNG · 최대 10MB",
  dropLabel: "파일을 이곳에 끌어다 놓거나",
  selectFile: "파일 선택",
  fileListLabel: "올린 파일",
  removeFile: "삭제",
  fileRequired: "원본 파일을 올려 주세요.",
  fileTypeInvalid: "PDF, JPG, PNG 파일만 올릴 수 있습니다.",
  fileTooLarge: "10MB 이하 파일만 올릴 수 있습니다.",
  photoTipsTitle: "사진으로 올릴 때 확인해 주세요",
  photoTips: [
    "모든 쪽이 잘리지 않고 한 장씩 보이게 찍어 주세요. 서명·날인 쪽도 포함해 주세요.",
    "그림자나 빛 반사로 글자가 가려지지 않게 해 주세요.",
    "AI가 읽지 못한 항목은 추측하지 않고 비워 두며, 다음 단계에서 직접 입력하거나 다시 올리도록 안내합니다.",
  ],

  workerTitle: "2. 근로자",
  workerLabel: "근로자 이름",
  workerHint:
    "Visafe에 가입한 근로자를 이름으로 찾아 확인 요청을 보냅니다. 이름 앞부분을 2글자 이상 입력해 주세요.",
  workerPlaceholder: "예: Nguyen Van An",
  search: "검색",
  searching: "찾는 중",
  searchTooShort: "이름을 2글자 이상 입력해 주세요.",
  workerResults: "검색 결과",
  workerResultsHint:
    "이름이 같은 근로자가 여럿이면 국적·생년월일·외국인등록번호 끝 4자리로 구분해 주세요.",
  noWorkers:
    "찾는 이름의 근로자가 없습니다. 근로자가 Visafe에 가입했는지 확인해 주세요.",
  workerRequired: "확인 요청을 보낼 근로자를 골라 주세요.",
  nationality: "국적",
  birthDate: "생년월일",
  alienRegNoLast4: "외국인등록번호 끝 4자리",

  noticeTitle: "원본 파일은 변경 감지에 쓰입니다",
  notice:
    "계약이 확정되면 원본 파일, 확정된 항목, 번역본을 묶은 해시가 기록되어 이후 바꿀 수 없습니다. 외국인등록번호나 여권번호가 적혀 있다면 가린 뒤 올려 주세요.",

  saveDraft: "임시 저장",
  next: "다음: AI 항목 추출",
  submitting: "올리는 중",
};

export type ContractNewLabels = typeof CONTRACT_NEW_LABELS;
