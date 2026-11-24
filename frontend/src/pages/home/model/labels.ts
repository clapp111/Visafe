/** 홈 화면 문구. 화면 언어(#54)가 붙으면 언어별 값으로 바꾼다. */
export const HOME_LABELS = {
  title: "근로계약 확인 결과 조회",
  description:
    "확인번호로 근로계약이 지금 유효한지, 누가 확인했는지 조회합니다. 조회 결과의 근로시간·계약기간을 가지고 계신 계약서와 비교해 보세요.",
  lookupForm: {
    label: "확인번호",
    placeholder: "예: VS-7K3F-9QXM",
    submit: "조회",
    required: "확인번호를 입력해 주세요.",
  },
  lookupHint:
    "계약이 확정될 때 발급되는 번호입니다. 대소문자는 구분하지 않고, 하이픈(-)은 자동으로 입력됩니다.",
  newNumberNotice:
    "근로조건이 바뀌면 기존 계약을 종료하고 새 계약을 맺으며, 새 확인번호가 발급됩니다.",
  quickTitle: "역할별 바로가기",
  quickDescription: "고용주와 근로자는 로그인한 뒤 이용할 수 있습니다.",
  quickLinks: {
    employer: {
      role: "고용주",
      title: "근로계약서 등록",
      description:
        "원본 계약서를 올리고 AI가 읽은 항목을 검토한 뒤, 근로자에게 확인을 요청합니다.",
      action: "등록하기",
    },
    worker: {
      role: "근로자",
      title: "계약 내용 확인",
      description:
        "모국어로 쉽게 풀어 쓴 계약 내용을 원본과 비교해 읽고, 이해했다는 확인을 남깁니다.",
      action: "확인하기",
    },
  },
  flowTitle: "계약 확인 절차",
  flowSteps: [
    {
      step: "1단계",
      title: "고용주 등록",
      description: "원본 업로드, AI 추출·검토",
    },
    {
      step: "2단계",
      title: "근로자 이해 확인",
      description: "원문과 번역 대조 후 확인",
    },
    {
      step: "3단계",
      title: "확정 · 블록체인 기록",
      description: "해시 기록, 확인번호 발급",
    },
  ],
  statusTitle: "조회 결과 상태 안내",
  statusDescription:
    "확인번호는 확정된 계약에만 발급되므로, 확정 전(진행 중)인 계약은 조회되지 않습니다. 상태는 색과 함께 항상 글자로 표시됩니다.",
  statusCaption: "조회 결과 상태",
  statusColumn: "상태",
  meaningColumn: "의미",
  confirmed: "확정",
  confirmedMeaning:
    "근로자가 확인하고 블록체인에 기록된, 현재 유효한 계약입니다.",
  terminated: "종료",
  terminatedMeaning:
    "더 이상 유효하지 않은 계약입니다. 종료 사유(기간 만료 또는 근로자·고용주 합의 종료)와 날짜가 함께 표시되며, 확인 기록은 이력으로 남습니다.",
};
