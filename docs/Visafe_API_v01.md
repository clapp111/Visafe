 Visafe API 명세서 (v0.1 초안)

Nov 18, 2026 · @동민

## 개요

현재 화면(와이어프레임 20개)에 붙일 API **33개**를 정의합니다. 기준은 DB 마이그레이션 V1·V2(테이블 23개)와 Visafe 와이어프레임이고, 이미 구현된 인증·회원 API 4개와, 화면이 없어 선택으로 둔 알림 API 3개를 포함합니다.

**이번 초안에 반영한 결정**

- **근로자 지정:** `members.name`으로 검색합니다. 이름이 같은 사람은 생년월일, 국적, 외국인등록번호 뒷자리 4개로 구분하고, 등록할 때는 검색 결과의 `workerMemberId`를 보냅니다. 화면의 "근로자 계정 ID" 입력란은 이름 검색과 선택 목록으로 바뀌어야 합니다.
- **계약서 양식:** 1종(`LABOR_CONTRACT`)만 씁니다. 등록 화면의 양식 선택 라디오 버튼은 빠지고 DB는 그대로입니다.
- **회원가입:** 이번 범위에서 뺍니다. 데모 계정은 시드 데이터로 넣고, 가입 API는 다음 버전에서 설계합니다.

**범위 밖:** 화면 언어 전환과 글자·화면 설정은 프론트에서 처리합니다(화면 언어 저장만 API 1개). 알림 API는 화면이 없어서 부록의 선택 항목으로 두었습니다.

## 공통 규칙

모든 API는 `/api/v1` 아래에 있고 JSON(camelCase)을 주고받습니다. 파일을 올리는 API만 `multipart/form-data`를 씁니다.

| 항목 | 규칙 |
| --- | --- |
| 인증 | `Authorization: Bearer <accessToken>` (유효 3시간). 리프레시 토큰은 HttpOnly 쿠키 (14일) |
| 로그인 없이 쓰는 API | `/api/v1/auth/**`, `/api/v1/lookup/**`. 조회 경로는 `SecurityConfig`의 `PUBLIC_PATHS`에 새로 추가해야 합니다 |
| 역할 | `EMPLOYER`, `WORKER`, `ADMIN`. `/api/v1/admin/**`는 ADMIN만 |
| 계약 접근 | 고용주는 같은 사업주(`company_id`)의 계약, 근로자는 본인 계약, 관리자는 전체. 볼 권한이 없는 계약은 있는지도 알 수 없게 404로 답합니다 |
| 날짜·시각 | 날짜 `2027-01-02`, 시각 `09:00`, 일시는 ISO-8601 UTC `2026-11-13T05:06:00Z`. 화면의 KST 표시는 프론트가 바꿉니다 |
| 금액 | 원 단위 정수 |
| 목록 | `page`(0부터), `size`(기본 20, 최대 100). 응답에 `page: {number, size, totalElements, totalPages}` |
| 근로조건 항목 키 | `contract_labor_terms` 컬럼 이름의 camelCase (`break_minutes` → `breakMinutes`). 19개 목록은 부록 A |

### 에러 응답

지금 구현은 `{ "code": 409, "message": "…" }` 형식이고, `code`에는 HTTP 상태값만 들어갑니다. 그래서 같은 409라도 원인을 구분할 수 없습니다. 이 명세는 `CustomErrorCode` 이름을 담는 `errorCode` 필드를 추가하는 것을 전제로 합니다. 에러 코드 목록은 부록 B에 있습니다.

```json
{ "code": 409, "errorCode": "CONTRACT_STAGE_CONFLICT", "message": "지금 단계에서는 할 수 없는 작업입니다." }
```

README에는 응답 형식이 RFC 9457이라고 적혀 있는데, 실제 코드와 다르니 README도 함께 고쳐야 합니다.

### 오래 걸리는 작업

AI 추출, 번역, 블록체인 기록은 요청하면 바로 `202 Accepted`와 바뀐 `stage`를 돌려주고, 작업은 백그라운드에서 계속됩니다. 프론트는 `GET /contracts/{contractId}`(M2)의 `stage`와 `processing`을 2\~3초 간격으로 확인합니다.

| 작업 | 시작하는 API | 진행 중 stage | 끝나면 | 실패하면 |
| --- | --- | --- | --- | --- |
| AI 추출 (OCR + 항목 읽기) | R1, R2, R3 | `UPLOADED` | `REVIEWING` | `UPLOADED`에 머물고 `EXTRACTION_FAILED` 이벤트를 남깁니다. R3로 다시 시도 |
| 번역 | R6, R7 | `TRANSLATING` | `AWAITING_WORKER` | 3회 실패하면 `translations.status = FAILED`. R7로 다시 시도 |
| 블록체인 기록 | W2(이해함), D3 | `RECORDING` | `DONE` + 확인번호 발급 | `chain_records.status = FAILED`. 서버가 다시 시도 (`retry_count`) |

```json
"processing": { "task": "TRANSLATION", "status": "RUNNING", "attempt": 2, "lastError": null }
```

`task`는 `EXTRACTION`, `TRANSLATION`, `CHAIN_RECORD` 중 하나이고, 진행 중인 작업이 없으면 `processing`은 `null`입니다. `status`는 `RUNNING` 또는 `FAILED`입니다.

## 엔드포인트 한눈에 보기

경로는 모두 `/api/v1` 뒤에 붙습니다. 번호는 아래 각 절의 번호와 같습니다.

| # | 메서드 | 경로 | 권한 | 화면 |
| --- | --- | --- | --- | --- |
| A1 | POST | `/auth/login` (구현됨) | 공개 | 로그인 |
| A2 | POST | `/auth/reissue` (구현됨) | 공개 | 공통 |
| A3 | POST | `/auth/logout` (구현됨) | 공개 | 공통 |
| A4 | GET | `/members/me` (구현됨) | 로그인 | 상단 사용자 메뉴 |
| A5 | PATCH | `/members/me` | 로그인 | 언어 변경 |
| A6 | GET | `/workers` | EMPLOYER | ② 1단계 근로자 지정 |
| L1 | GET | `/lookup/{confirmationNo}` | 공개 | ① 조회 결과 4종 |
| L2 | POST | `/lookup/{confirmationNo}/file-check` | 공개 | ① 파일 대조 |
| R1 | POST | `/contracts` | EMPLOYER | ② 1단계 업로드 |
| R2 | PUT | `/contracts/{contractId}/file` | EMPLOYER | ② 원본 다시 올리기 |
| R3 | POST | `/contracts/{contractId}/extraction/retry` | EMPLOYER | ② 추출 실패 시 |
| R4 | GET | `/contracts/{contractId}/draft` | EMPLOYER | ② 2단계 검토 |
| R5 | PATCH | `/contracts/{contractId}/draft/terms` | EMPLOYER | ② 2단계 항목 수정 |
| R6 | POST | `/contracts/{contractId}/submit` | EMPLOYER | ② 제출 확인 창 |
| R7 | POST | `/contracts/{contractId}/translations/retry` | EMPLOYER | 번역 실패 시 |
| R8 | POST | `/contracts/{contractId}/revisions` | EMPLOYER | ⑤ 수정하기 (돌아온 계약) |
| W1 | GET | `/contracts/{contractId}/review` | WORKER | ③ 계약 내용 확인 |
| W2 | POST | `/contracts/{contractId}/review-responses` | WORKER | ③ 이해했습니다 / 신고 창 |
| W3 | GET | `/contracts/{contractId}/questions` | WORKER | ③ 질문하기 |
| W4 | POST | `/contracts/{contractId}/questions` | WORKER | ③ 질문하기 |
| M1 | GET | `/contracts` | EMPLOYER, WORKER | ⑤ 내 계약 목록 |
| M2 | GET | `/contracts/{contractId}` | 당사자, ADMIN | ⑤ 상세, 완료 화면, 진행 확인 |
| M3 | GET | `/contracts/{contractId}/files/{fileId}/content` | 당사자, ADMIN | 원본 보기·내려받기 |
| M4 | POST | `/contracts/{contractId}/terminations` | 당사자 | ⑤ 종료 요청 |
| M5 | POST | `/contracts/{contractId}/terminations/{terminationId}/response` | 상대 당사자 | ⑤ 종료 동의·거절 |
| M6 | POST | `/contracts/{contractId}/terminations/{terminationId}/cancel` | 요청한 사람 | ⑤ 종료 요청 취소 |
| D1 | GET | `/admin/contracts` | ADMIN | ④ 확인 대기 목록 |
| D2 | GET | `/admin/contracts/{contractId}` | ADMIN | ④ 계약 검토 |
| D3 | POST | `/admin/contracts/{contractId}/approve` | ADMIN | ④ 확정 확인 창 |
| D4 | POST | `/admin/contracts/{contractId}/reject` | ADMIN | ④ 반려 사유 창 |
| N1 | GET | `/notifications` | 로그인 | (화면 없음, 선택) |
| N2 | PATCH | `/notifications/{notificationId}/read` | 로그인 | (화면 없음, 선택) |
| N3 | POST | `/notifications/read-all` | 로그인 | (화면 없음, 선택) |

## 인증·회원 API

A1\~A4는 이미 구현되어 있어 그대로 씁니다. 로그인하면 `{grantType, accessToken, expiresIn}`을 받고 리프레시 토큰은 쿠키로 받으며, A4는 `{id, email, name, role, uiLanguage}`를 돌려줍니다.

### A5. PATCH /members/me — 화면 언어 저장

상단의 언어 버튼을 누르면 호출합니다. 바꿀 수 있는 값은 `uiLanguage`(`ko`, `vi`) 하나입니다.

```json
// 요청
{ "uiLanguage": "vi" }
// 응답 200: A4와 같은 형식
```

### A6. GET /workers?name={이름} — 근로자 검색

계약 등록 1단계에서 확인 요청을 보낼 근로자를 고릅니다. `members.name`을 대소문자와 베트남어 성조 부호를 무시하고 앞부분이 일치하는 사람을 찾습니다. 그래서 `Nguyen`으로 검색해도 `Nguyễn`이 나오며, PostgreSQL의 `unaccent` 확장을 씁니다. `role = WORKER`만 돌려주고, 검색어는 2글자 이상이며 결과는 최대 20건입니다. 이 방식은 `ix_members_name` 인덱스를 타지 않지만, 프로토타입 데이터 양에서는 문제가 없습니다.

```json
// 응답 200
[
  { "memberId": 12, "name": "Nguyễn Văn An", "nationality": "VN", "birthDate": "1998-04-17", "alienRegNoLast4": "4821" }
]
```

- 같은 이름은 `birthDate`와 `alienRegNoLast4`로 구분합니다(`member_worker_profiles`).
- 고용주라면 누구나 근로자의 생년월일을 볼 수 있습니다. 프로토타입에서는 그대로 두되, 실제 서비스라면 생년월일을 입력받아 일치하는 사람만 보여 주는 방식이 안전합니다.
- 에러: `400 COMMON_INVALID_PARAMETER` (2글자 미만)

## ① 공개 조회 API

로그인 없이 확인번호 하나로 계약의 유효 여부를 확인합니다. 확인번호는 확정된 계약(`DONE`, `ENDED`)에만 있으므로, 진행 중인 계약은 항상 "번호 없음"으로 나옵니다.

### L1. GET /lookup/{confirmationNo} — 확인번호 조회

조회 결과 화면 4종(확정 · 파일 불일치 · 종료 · 번호 없음)이 모두 이 API 하나를 씁니다. 파일 불일치 화면은 L2의 결과로 나뉩니다.

- 서버는 입력을 대문자로 바꾸고 하이픈을 맞춘 다음 `^VS-[2-9A-HJ-NP-Z]{4}-[2-9A-HJ-NP-Z]{4}$` 형식인지 확인합니다.
- 임금을 포함한 전체 항목은 계약 당사자만 볼 수 있으므로(M2), 여기서는 근로시간과 계약기간만 공개합니다.
- 근로자 이름은 마지막 단어만 남기고 각 단어의 첫 글자 뒤를 `*`로 가립니다 (`Nguyễn Văn An` → `N***** V** An`).

```json
// 응답 200
{
  "confirmationNo": "VS-7K3F-9QXM",
  "status": "CONFIRMED",
  "companyName": "한빛정밀",
  "workerNameMasked": "N***** V** An",
  "startDate": "2027-01-02",
  "endDate": "2029-12-31",
  "confirmedAt": "2026-11-13T05:06:00Z",
  "keyTerms": {
    "workStartTime": "09:00", "workEndTime": "18:00", "breakMinutes": 60,
    "workDaysPerWeek": 5, "workDaysText": "월~금", "weeklyHours": 40
  },
  "integrity": { "status": "VERIFIED", "checkedAt": "2026-11-18T10:12:00Z" },
  "termination": null,
  "chainRecords": [
    { "recordType": "CONFIRM", "status": "MINED", "network": "sepolia",
      "txHash": "0x4c1e…9a7b", "minedAt": "2026-11-13T05:06:00Z", "explorerUrl": "https://sepolia.etherscan.io/tx/0x4c1e…9a7b" }
  ],
  "history": [
    { "type": "SUBMITTED", "occurredAt": "2026-11-13T04:24:00Z", "actorLabel": "한빛정밀" },
    { "type": "WORKER_UNDERSTOOD", "occurredAt": "2026-11-13T05:05:00Z", "detail": { "language": "vi" } },
    { "type": "CONFIRMED", "occurredAt": "2026-11-13T05:06:00Z" }
  ]
}
```

| 필드 | 설명 |
| --- | --- |
| `status` | `CONFIRMED` 또는 `TERMINATED` (`contracts.status`) |
| `weeklyHours` | (종업 − 시업 − 휴게) × 주 근무일 수로 서버가 계산 |
| `integrity.status` | `VERIFIED`: DB의 해시 구성 요소로 다시 계산한 `record_hash`가 온체인 값과 같음 · `MISMATCH`: 다름 · `PENDING`: 아직 블록에 포함되지 않음 · `UNAVAILABLE`: 체인 조회 실패. 화면의 "확정 이후 바뀐 기록이 없습니다" 문구가 이 값에서 나옵니다 |
| `termination` | 종료된 계약일 때 `{ reasonType, endDate, terminatedAt }` (`EXPIRED`, `MUTUAL`, `REPLACED`) |
| `history` | 확정된 회차의 공개 이벤트만 보여 줍니다: `SUBMITTED`, `WORKER_UNDERSTOOD`, `AUTHORITY_APPROVED`, `CONFIRMED`, `TERMINATED`. 수정 이력이나 신고 같은 내부 이벤트는 넣지 않습니다 |

- 에러: `400 LOOKUP_INVALID_FORMAT` · `404 LOOKUP_NOT_FOUND` (번호 없음 화면)
- 확인번호를 마구 대입해 보는 시도를 막으려면 IP당 요청 수 제한이 필요합니다 (예: 분당 30회).

### L2. POST /lookup/{confirmationNo}/file-check — 파일 대조

`multipart/form-data`의 `file` 하나(최대 10MB)를 받아 keccak256 해시를 계산하고, 확정 기록(`chain_records`의 CONFIRM)에 있는 `file_hash`와 비교합니다. 파일은 저장하지 않습니다.

```json
// 응답 200
{
  "match": false,
  "uploadedFileHash": "0x51d0e9b4…a72f0e3b8",
  "recordedFileHash": "0x8f3a2c71…9c3e7c21d"
}
```

- `match: false`이면 화면은 불일치 안내(다시 스캔한 파일은 값이 달라진다는 설명과 1350 문의)를 보여 줍니다.
- 에러: `404 LOOKUP_NOT_FOUND` · `413 FILE_TOO_LARGE`

## ② 고용주 계약 등록 API

고용주는 다음 순서로 진행합니다. 원본을 올리고(R1), AI가 읽은 항목을 검토·수정한 뒤(R4, R5) 제출합니다(R6). 근로자나 기관이 돌려보낸 계약은 R8로 새 회차를 열고 같은 화면에서 고칩니다.

### R1. POST /contracts — 계약 생성과 원본 업로드

1단계의 "다음: AI 항목 추출" 버튼과 "임시 저장" 버튼은 둘 다 이 API를 호출합니다. 계약을 만드는 것 자체가 저장이므로, 두 버튼은 이동할 화면만 다릅니다.

`multipart/form-data` 파트:

| 파트 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `file` | 파일 | 예 | PDF, JPG, PNG · 최대 10MB · 1개 |
| `data` | JSON | 예 | `{ "workerMemberId": 12, "previousContractId": null }` |

- `workerMemberId`는 A6에서 고른 근로자입니다. `previousContractId`는 조건이 바뀌어 기존 확정 계약을 대체할 때만 보냅니다.
- 서버가 채우는 값: `company_id`는 로그인한 고용주의 사업주, `target_language`는 근로자의 `preferred_language`(MVP는 `vi`), `doc_type`은 `LABOR_CONTRACT`, `contract_no`는 `C-연도-일련번호5자리`입니다.
- 만드는 행: `contracts`(UPLOADED), `contract_files`(keccak256 `file_hash` 포함), `contract_versions`(1회차 DRAFT), 이벤트 `CONTRACT_CREATED`와 `FILE_UPLOADED`. 그다음 비동기로 AI 추출을 시작합니다.
- 추출이 끝나면 `contract_clauses`, `contract_labor_terms`, `contract_term_annotations`가 채워지고 stage가 `REVIEWING`으로 바뀝니다. 이때 이벤트 `EXTRACTION_COMPLETED`가 남습니다.

```json
// 응답 201 (Location: /api/v1/contracts/31)
{ "contractId": 31, "contractNo": "C-2026-04817", "stage": "UPLOADED",
  "processing": { "task": "EXTRACTION", "status": "RUNNING", "attempt": 1, "lastError": null } }
```

에러: `404 WORKER_NOT_FOUND` (없거나 WORKER가 아님) · `413 FILE_TOO_LARGE` · `415 FILE_UNSUPPORTED_TYPE` · `409 PREVIOUS_CONTRACT_INVALID` (대체할 계약이 확정 상태가 아니거나 사업주·근로자가 다름)

### R2. PUT /contracts/{contractId}/file — 원본 다시 올리기

2단계 화면의 "이전: 원본 다시 올리기" 버튼이 호출합니다. 파트는 `file` 하나입니다.

- 현재 회차가 DRAFT일 때(stage `UPLOADED` 또는 `REVIEWING`)만 할 수 있습니다.
- 새 `contract_files` 행을 만들고 회차의 `contract_file_id`를 바꿉니다. 항목 값과 부가 정보는 비우고 다시 추출합니다. 예전 파일은 기록 보존 원칙에 따라 지우지 않습니다.
- stage는 `REVIEWING → UPLOADED`로 바뀝니다. 지금 `ContractStage.canTransitionTo`에는 이 전이가 없으므로 추가해야 합니다(부록 C).
- 응답은 202이며 형식은 R1과 같습니다. 에러: `409 CONTRACT_STAGE_CONFLICT` 외 R1과 같음

### R3. POST /contracts/{contractId}/extraction/retry — 추출 다시 시도

stage가 `UPLOADED`이고 마지막 추출이 실패했을 때만 할 수 있습니다. 본문 없이 호출하며 응답은 202입니다. 추출 상태를 담는 컬럼이 없으므로, 실패 여부는 마지막 추출 이벤트가 `EXTRACTION_FAILED`인지로 판단합니다.

### R4. GET /contracts/{contractId}/draft — 검토할 항목 조회

2단계 "계약 내용 검토 · 수정" 화면의 데이터입니다. 현재 회차를 기준으로 하고, 돌아온 계약이면 신고 내용(`returned`)도 함께 줍니다.

```json
// 응답 200
{
  "contractId": 31, "contractNo": "C-2026-04817", "stage": "REVIEWING", "versionNo": 1,
  "processing": null,
  "worker": { "memberId": 12, "name": "Nguyễn Văn An", "nationality": "VN" },
  "file": { "fileId": 44, "fileName": "표준근로계약서_한빛정밀.pdf", "mimeType": "application/pdf", "sizeBytes": 1258291, "pageCount": 2 },
  "fields": [
    { "key": "breakMinutes", "label": "휴게시간(분)", "type": "INTEGER", "required": true,
      "value": null, "aiValue": null, "valueSource": "AI", "extractionStatus": "UNREAD",
      "warning": null, "sourceClause": null },
    { "key": "monthlyBaseWage", "label": "월 기본급(원)", "type": "INTEGER", "required": true,
      "value": 2000000, "aiValue": "2000000", "valueSource": "AI", "extractionStatus": "READ",
      "warning": { "code": "MIN_WAGE", "message": "주 40시간(월 209시간) 기준 환산 시급 약 9,569원으로, 2026년 최저임금 시급 10,320원보다 낮습니다.",
                   "detail": { "hourlyWage": 9569, "minimumWage": 10320, "year": 2026 } },
      "sourceClause": { "clauseId": 208, "clauseNo": "제6조", "pageNo": 1 } }
  ],
  "attention": [
    { "key": "breakMinutes", "reason": "UNREAD" },
    { "key": "monthlyBaseWage", "reason": "MIN_WAGE" }
  ],
  "returned": null
}
```

| 필드 | 설명 |
| --- | --- |
| `fields` | 19개 항목 전부를 부록 A의 순서대로 줍니다. `value`는 `contract_labor_terms`, 나머지는 `contract_term_annotations`에서 옵니다 |
| `valueSource` | `AI`, `EMPLOYER_EDIT`(AI 값을 고침), `EMPLOYER_INPUT`(AI가 못 읽어 직접 입력). 화면의 "직접 수정함" 배지가 여기서 나옵니다 |
| `warning.message` | 서버가 `warning_code`로 만드는 문구입니다 (DB에는 코드만 저장) |
| `attention` | 화면 상단의 "확인이 필요한 항목 N개" 목록입니다. `UNREAD`, `MIN_WAGE`, `NEEDS_CHECK`, `REPORTED` |
| `returned` | 돌아온 계약일 때만 채워집니다. 아래 예시 참고 |

```json
"returned": {
  "by": "WORKER", "at": "2026-11-13T05:20:00Z",
  "reportedFields": ["breakMinutes"],
  "reason": "Trong bản gốc ghi 30 phút.",
  "reasonKo": "원본에는 휴게시간이 30분으로 적혀 있어요."
}
```

기관이 반려한 경우에는 `by: "AUTHORITY"`이고 `reason`에 반려 사유가 들어갑니다. 에러: `409 CONTRACT_STAGE_CONFLICT` (stage가 `UPLOADED`, `REVIEWING`, `RETURNED`가 아님)

### R5. PATCH /contracts/{contractId}/draft/terms — 항목 수정

바꾼 항목만 보냅니다. 값을 비우려면 `null`을 보냅니다. 폼을 벗어날 때나 제출 직전에 한 번에 보내도 됩니다.

```json
// 요청
{ "values": { "breakMinutes": 60, "jobDescription": "금속 부품 가공 및 포장" } }
// 응답 200: R4와 같은 형식 (경고 다시 계산)
```

- stage가 `REVIEWING`이고 현재 회차가 DRAFT일 때만 할 수 있습니다.
- 값이 실제로 바뀐 항목마다 `contract_term_edit_logs`에 수정 전·후 값을 남기고 이벤트 `TERM_EDITED`를 남깁니다.
- `valueSource`는 서버가 정합니다. AI가 못 읽은 항목이면 `EMPLOYER_INPUT`, AI 값과 다르면 `EMPLOYER_EDIT`, AI 값으로 되돌리면 다시 `AI`입니다.
- 값 검사는 DB CHECK와 같습니다: 종료일 ≥ 시작일, 휴게 0\~1440분, 주 근무일 1\~7, 지급일 1\~31, 기본급 ≥ 0, 국적은 ISO 2자리 대문자.
- 에러: `400 TERM_VALUE_INVALID` (어느 항목이 왜 틀렸는지 `message`에 담음) · `400 TERM_KEY_UNKNOWN` · `409 CONTRACT_STAGE_CONFLICT`

### R6. POST /contracts/{contractId}/submit — 제출하고 확인 요청 보내기

제출 확인 창에서 호출합니다. "원본과 같음을 확인했습니다" 체크 여부를 함께 보냅니다.

```json
// 요청
{ "confirmedOriginalMatch": true }
// 응답 202
{ "contractId": 31, "stage": "TRANSLATING", "versionNo": 1,
  "processing": { "task": "TRANSLATION", "status": "RUNNING", "attempt": 1, "lastError": null } }
```

- 부록 A의 필수 항목이 하나라도 비어 있으면 거절합니다. 경고(최저임금 등)는 제출을 막지 않습니다.
- 처리 순서는 이렇습니다. `items_hash`를 계산하고, 회차를 `SUBMITTED`로 바꾸며 제출자와 제출 일시를 기록합니다. stage는 `TRANSLATING`이 되고 이벤트 `SUBMITTED`를 남긴 뒤, `translations` 행(PENDING)을 만들고 번역을 시작합니다.
- 번역이 끝나면 stage는 `AWAITING_WORKER`가 되고, 이벤트 `TRANSLATION_COMPLETED`와 근로자 알림 `REVIEW_REQUESTED`가 남습니다.
- 에러: `400 CONFIRMATION_REQUIRED` (체크 안 함) · `422 TERMS_REQUIRED_MISSING` (빈 항목 키를 `message`에 담음) · `409 CONTRACT_STAGE_CONFLICT`

### R7. POST /contracts/{contractId}/translations/retry — 번역 다시 시도

번역이 3회 모두 실패해 고용주가 `TRANSLATION_FAILED` 알림을 받은 뒤에 호출합니다. 같은 회차에 `translation_no + 1`인 새 번역본을 만들고 202를 돌려줍니다. 에러: `409 CONTRACT_STAGE_CONFLICT` (stage가 `TRANSLATING`이 아니거나 마지막 번역이 실패가 아님)

### R8. POST /contracts/{contractId}/revisions — 돌아온 계약 수정 시작

내 계약 목록의 "수정하기" 버튼이 호출하고, 이어서 R4 화면으로 이동합니다.

- stage가 `RETURNED`일 때 새 회차(`version_no + 1`, DRAFT)를 만듭니다. 이전 회차의 파일, 항목 값, 부가 정보를 복사하고 `current_contract_version_id`를 바꾼 뒤 stage를 `REVIEWING`으로 둡니다.
- 이미 `REVIEWING`이면 아무것도 바꾸지 않고 현재 회차를 돌려줍니다. 두 번 눌러도 안전합니다.
- 응답 200: `{ "contractId": 31, "stage": "REVIEWING", "versionNo": 2 }`

## ③ 근로자 확인 API

근로자는 모국어 번역본과 원본을 비교해 읽고(W1), 궁금한 점을 묻고(W3, W4), "이해했습니다" 또는 "원본과 다릅니다"로 응답합니다(W2). 네 API 모두 해당 계약의 근로자 본인만 호출할 수 있습니다.

### W1. GET /contracts/{contractId}/review — 확인할 계약 내용

현재 회차의 마지막 번역본 중 완료(`DONE`)된 것을 기준으로 합니다. stage가 `AWAITING_WORKER`가 아니어도 읽을 수는 있지만, 그때는 `canRespond: false`입니다.

```json
// 응답 200
{
  "contractId": 31, "contractNo": "C-2026-04817", "status": "IN_PROGRESS", "stage": "AWAITING_WORKER",
  "canRespond": true,
  "employer": { "companyName": "한빛정밀", "representativeName": "김민수", "memberName": "김민수" },
  "requestedAt": "2026-11-13T04:30:00Z",
  "translationId": 57, "language": "vi",
  "file": { "fileId": 44, "mimeType": "application/pdf", "pageCount": 2 },
  "keyTerms": {
    "monthlyBaseWage": 2000000, "payDay": 10, "payMethod": "본인 명의 계좌 입금",
    "workStartTime": "09:00", "workEndTime": "18:00", "workDaysText": "월~금", "weeklyHours": 40, "breakMinutes": 60,
    "startDate": "2027-01-02", "endDate": "2029-12-31"
  },
  "warnings": [
    { "code": "MIN_WAGE", "key": "monthlyBaseWage", "message": "…(근로자 언어)", "messageKo": "월 기본급이 최저임금보다 낮을 수 있습니다…",
      "detail": { "hourlyWage": 9569, "minimumWage": 10320, "year": 2026 } }
  ],
  "items": [
    { "key": "breakMinutes", "label": "휴게시간(분)", "value": 60, "translatedValue": "60 phút",
      "easyExplanation": "Bạn được nghỉ 1 tiếng…", "easyExplanationKo": "일하는 중간에 1시간 쉽니다…",
      "valueSource": "EMPLOYER_INPUT",
      "employerInput": { "aiValue": null, "extractionStatus": "UNREAD" },
      "sourceClause": { "clauseId": 211, "clauseNo": "제4조", "pageNo": 1 } }
  ],
  "clauses": [
    { "clauseId": 211, "seq": 4, "clauseNo": "제4조", "pageNo": 1, "textKo": "…", "translatedText": "…" }
  ]
}
```

| 필드 | 설명 |
| --- | --- |
| `requestedAt` | 번역이 끝나 확인 요청이 간 시각 (`translations.completed_at`) |
| `keyTerms` | "꼭 확인할 3가지" 카드(임금, 근로시간, 계약기간) |
| `items` | 19개 항목. 번역과 쉬운 설명은 `translation_items`에서 옵니다. 값이 빈 항목은 빼고 보냅니다 |
| `employerInput` | `valueSource`가 `AI`가 아닐 때만 들어갑니다. "고용주 입력 자세히"의 "AI가 읽은 값 / 고용주가 입력한 값"에 씁니다 |
| `sourceClause.pageNo` | "원본 1쪽 보기" 링크. 원본 파일은 M3로 받아 프론트가 그립니다 |

에러: `404 CONTRACT_NOT_FOUND` · `409 TRANSLATION_NOT_READY` (완료된 번역본이 없음)

### W2. POST /contracts/{contractId}/review-responses — 이해함 / 원본과 다름

하나의 API로 두 버튼을 모두 처리합니다. `translationId`는 W1에서 받은 값을 그대로 보내야 하며, 그사이 다른 번역본이 나오면 거절됩니다.

```json
// 요청: 이해했습니다
{ "translationId": 57, "action": "UNDERSTOOD", "readAllChecked": true }

// 요청: 원본과 다릅니다
{ "translationId": 57, "action": "REPORTED",
  "reportItems": [ { "type": "ITEM_MISMATCH", "key": "breakMinutes" } ],
  "reason": "Trong bản gốc ghi 30 phút." }

// 응답 202
{ "contractId": 31, "stage": "RECORDING",
  "processing": { "task": "CHAIN_RECORD", "status": "RUNNING", "attempt": 1, "lastError": null } }
```

| 응답 | 그다음 stage | 서버가 하는 일 |
| --- | --- | --- |
| `UNDERSTOOD`, 기관 검토 안 함 | `RECORDING` → `DONE` | 이벤트 `WORKER_UNDERSTOOD`를 남기고 확인번호를 만들어 `chain_records`(CONFIRM)로 기록합니다. 블록에 포함되면 `DONE`, 회차 `CONFIRMED`, `confirmed_at`, 이벤트 `CONFIRMED`, 양쪽 알림 `CONTRACT_CONFIRMED` |
| `UNDERSTOOD`, 기관 검토 함 | `AUTHORITY_REVIEW` | 이벤트 `AUTHORITY_REVIEW_REQUESTED`. 이후는 D3·D4가 진행 |
| `REPORTED` | `RETURNED` | 회차 `RETURNED`, 이벤트 `WORKER_REPORTED`, 고용주 알림 `CONTRACT_RETURNED`. `reason`은 한국어로 번역해 R4의 `returned.reasonKo`로 보여 줍니다 |

- `UNDERSTOOD`는 `readAllChecked: true`가 필수입니다(DB CHECK). `REPORTED`는 `reportItems`가 1개 이상 필요합니다.
- `reportItems[].type`은 `ITEM_MISMATCH` 하나뿐이며 `key`가 필수입니다. 번역 신고는 MVP에서 제외했습니다.
- 신고 창에서 묶어 보여 주는 항목은 프론트가 키 여러 개로 풀어 보냅니다. 예를 들어 "지급일 · 지급 방법"은 `payDay`와 `payMethod`, "근로시간"은 `workStartTime`, `workEndTime`, `workDaysPerWeek`, `workDaysText`입니다.
- 확인 완료 화면은 M2를 다시 호출해 `stage: DONE`과 `confirmationNo`가 나오면 보여 줍니다.
- 에러: `400 READ_ALL_REQUIRED` · `400 REPORT_ITEMS_REQUIRED` · `409 TRANSLATION_OUTDATED` · `409 CONTRACT_STAGE_CONFLICT`

### W3. GET /contracts/{contractId}/questions — 질문 목록

현재 회차에서 내가 한 질문을 오래된 순서로 돌려줍니다. 항목 형식은 W4의 응답과 같습니다.

### W4. POST /contracts/{contractId}/questions — 질문하기

답은 이 계약서의 조항만 근거로 하며, 근거가 된 원문 문장을 함께 돌려줍니다. AI가 바로 답하는 동기 방식이며, 최대 20초까지 기다립니다.

```json
// 요청
{ "question": "야근하면 돈을 더 받나요?" }
// 응답 201
{
  "questionId": 9, "language": "vi", "question": "야근하면 돈을 더 받나요?",
  "answer": "네. 계약서에는 연장·야간·휴일에 일하면 …",
  "referredToCenter": true,
  "citations": [ { "clauseId": 215, "clauseNo": "제8조", "pageNo": 2, "quotedText": "연장·야간·휴일근로 시 …" } ],
  "askedAt": "2026-11-13T04:40:00Z", "answeredAt": "2026-11-13T04:40:06Z"
}
```

- 질문 언어는 근로자의 `preferred_language`입니다. 법률 판단이 필요한 답이면 `referredToCenter: true`이고, 화면에 1350 안내를 보여 줍니다.
- AI가 실패하거나 시간이 초과되면 질문만 저장하고 `answer: null`을 돌려줍니다. 그러면 화면은 "다시 질문해 주세요"를 보여 줍니다.
- 에러: `400 COMMON_INVALID_REQUEST` (빈 질문, 500자 초과)

## ⑤ 내 계약·종료 API

고용주와 근로자가 같은 API를 씁니다. 서버가 로그인한 사람의 역할에 맞춰 범위, 상대방 정보, 설명 언어를 바꿉니다.

### M1. GET /contracts?status={상태}&page=&size= — 내 계약 목록

`status`는 `IN_PROGRESS`, `CONFIRMED`, `TERMINATED` 중 하나이고, 빼면 전체를 봅니다. 정렬은 할 일이 있는 계약이 먼저이고, 그다음은 최근 수정 순입니다.

```json
// 응답 200 (고용주)
{
  "counts": { "all": 5, "inProgress": 2, "confirmed": 2, "terminated": 1 },
  "todoCount": 1,
  "content": [
    { "contractId": 35, "contractNo": "C-2026-04833", "counterpartyName": "Lê Thị Hoa",
      "startDate": "2027-01-02", "endDate": "2028-12-31",
      "status": "IN_PROGRESS", "stage": "RETURNED", "todo": "FIX_REQUIRED", "updatedAt": "2026-11-13T05:20:00Z" }
  ],
  "page": { "number": 0, "size": 20, "totalElements": 5, "totalPages": 1 }
}
```

| 역할 | 범위 | `counterpartyName` | `todo` 값 |
| --- | --- | --- | --- |
| EMPLOYER | 같은 사업주의 모든 계약 | 근로자 이름 | `FIX_REQUIRED`(RETURNED), `REVIEW_INCOMPLETE`(REVIEWING), `EXTRACTION_FAILED`, `TRANSLATION_FAILED`, `TERMINATION_RESPONSE` |
| WORKER | 본인 계약 | 사업체명 | `CONFIRM_REQUIRED`(AWAITING\_WORKER), `TERMINATION_RESPONSE` |

- 계약기간은 현재 회차의 `contract_labor_terms`에서 가져오며, 추출 전이면 `null`입니다.
- `todoCount`는 "수정이 필요한 계약이 1건 있습니다" 배너에, `counts`는 상태 필터 칩의 숫자에 씁니다.

### M2. GET /contracts/{contractId} — 계약 상세

"내 계약 · 상세" 화면의 데이터입니다. 확인 완료, 제출 완료, 확정 완료 화면이 진행 상황을 확인할 때도 이 API를 씁니다.

```json
// 응답 200 (근로자)
{
  "contractId": 31, "contractNo": "C-2026-04817",
  "status": "CONFIRMED", "stage": "DONE", "processing": null,
  "confirmationNo": "VS-7K3F-9QXM", "confirmedAt": "2026-11-13T05:06:00Z", "terminatedAt": null,
  "employer": { "companyName": "한빛정밀", "representativeName": "김민수" },
  "worker": { "memberId": 12, "name": "Nguyễn Văn An", "nationality": "VN" },
  "previousContractId": null,
  "file": { "fileId": 44, "fileName": "원본_근로계약서.pdf", "mimeType": "application/pdf", "sizeBytes": 1258291, "pageCount": 2 },
  "terms": [
    { "key": "breakMinutes", "label": "휴게시간(분)", "value": 60, "valueSource": "EMPLOYER_INPUT",
      "explanation": "일하는 중간에 1시간 쉽니다." }
  ],
  "timeline": [
    { "type": "SUBMITTED", "occurredAt": "2026-11-13T04:24:00Z", "actorLabel": "한빛정밀" },
    { "type": "WORKER_UNDERSTOOD", "occurredAt": "2026-11-13T05:05:00Z", "detail": { "language": "vi" } },
    { "type": "CONFIRMED", "occurredAt": "2026-11-13T05:06:00Z" }
  ],
  "chainRecords": [ { "recordType": "CONFIRM", "status": "MINED", "network": "sepolia", "txHash": "0x4c1e…9a7b",
                      "minedAt": "2026-11-13T05:06:00Z", "explorerUrl": "https://sepolia.etherscan.io/tx/0x4c1e…9a7b" } ],
  "termination": null,
  "availableActions": ["DOWNLOAD_FILE", "REQUEST_TERMINATION"]
}
```

| 필드 | 설명 |
| --- | --- |
| `terms` | 확정된 회차가 있으면 그 회차, 없으면 현재 회차의 항목입니다. `explanation`은 근로자에게는 `easy_explanation`(모국어), 고용주에게는 `easy_explanation_ko`를 줍니다. 형광펜 표시는 `valueSource`로 판단합니다 |
| `timeline` | `contract_events`를 시간 순서로 보여 줍니다. 당사자에게는 신고와 반려도 보여 주고, `TERM_EDITED`는 묶어서 한 줄로 줍니다 |
| `termination` | 진행 중이거나 끝난 종료. 예: `{ terminationId, reasonType, reasonText, endDate, status, requestedByMe }` |
| `availableActions` | 로그인한 사람이 지금 할 수 있는 일입니다. 화면은 이 값에 따라 버튼을 보여 줍니다: `EDIT_DRAFT`, `START_REVISION`, `RETRY_EXTRACTION`, `RETRY_TRANSLATION`, `RESPOND_REVIEW`, `DOWNLOAD_FILE`, `REQUEST_TERMINATION`, `RESPOND_TERMINATION`, `CANCEL_TERMINATION` |

### M3. GET /contracts/{contractId}/files/{fileId}/content — 원본 파일

저장된 바이트를 그대로 내려줍니다. `Content-Type`은 `mime_type`, `Content-Disposition`은 `attachment; filename*=UTF-8''…`입니다. 화면에 미리 보여 줄 때는 `?inline=true`를 붙입니다. 원본 미리보기의 쪽 넘기기는 프론트(pdf.js)가 처리합니다.

- "등록된 원본 내려받기"로 받은 파일로 L2 대조가 되려면, 서버는 파일을 절대 다시 인코딩하거나 압축하지 않아야 합니다.
- 에러: `404 FILE_NOT_FOUND` (다른 계약의 파일인 경우 포함)

### M4. POST /contracts/{contractId}/terminations — 종료 요청

확정된 계약(`DONE`)을 기간보다 일찍 끝낼 때 씁니다(합의 종료). 근로자와 고용주 어느 쪽이든 요청할 수 있습니다.

```json
// 요청
{ "endDate": "2027-06-30", "reasonText": "귀국 예정" }
// 응답 201
{ "terminationId": 3, "reasonType": "MUTUAL", "status": "REQUESTED", "endDate": "2027-06-30" }
```

- `contract_terminations`(MUTUAL, REQUESTED)에 요청자와 요청 시각을 넣고, 이벤트 `TERMINATION_REQUESTED`와 상대방 알림 `TERMINATION_REQUESTED`를 남깁니다.
- `endDate`는 오늘 이후이고 계약 종료일 이전이어야 합니다.
- 에러: `409 TERMINATION_ALREADY_ACTIVE` (`uq_contract_terminations_active`) · `409 CONTRACT_STAGE_CONFLICT` · `400 TERMINATION_DATE_INVALID`

### M5. POST /contracts/{contractId}/terminations/{terminationId}/response — 동의·거절

요청한 사람의 상대방만 호출할 수 있습니다(DB CHECK `requester_not_responder`).

```json
// 요청
{ "decision": "AGREE" }   // 또는 "DECLINE"
// 응답 200
{ "terminationId": 3, "status": "AGREED", "endDate": "2027-06-30" }
```

- `AGREE`하면 status는 `AGREED`가 되고 이벤트 `TERMINATION_AGREED`가 남습니다. 종료일이 되면 스케줄러가 `chain_records`(TERMINATE)를 기록하고 계약을 `ENDED`로 바꿉니다. 이때 `terminated_at`, 이벤트 `TERMINATED`, 양쪽 알림 `CONTRACT_TERMINATED`가 남습니다.
- `DECLINE`하면 status는 `DECLINED`가 되고 이벤트 `TERMINATION_DECLINED`와 요청자 알림 `TERMINATION_RESPONDED`가 남습니다.
- 기간 만료(`EXPIRED`)와 새 계약으로의 대체(`REPLACED`)는 서버가 자동으로 만드는 종료이므로 API가 없습니다. 두 경우 모두 처음부터 `AGREED` 상태입니다.
- 에러: `403 TERMINATION_NOT_ALLOWED` (요청자 본인) · `409 TERMINATION_STATUS_CONFLICT` (이미 응답함)

### M6. POST /contracts/{contractId}/terminations/{terminationId}/cancel — 요청 취소

요청한 사람만, 상태가 `REQUESTED`일 때 할 수 있습니다. status는 `CANCELED`가 되고 이벤트 `TERMINATION_CANCELED`가 남으며, 응답은 M5와 같은 형식입니다.

## ④ 감독 기관 API (선택 단계)

`contracts.requires_authority_review`가 `true`인 계약만 이 단계를 거칩니다. 값은 계약을 만들 때 서버 설정 `visafe.authority-review.enabled`(기본 `false`)에서 복사하는 것을 제안합니다. 기관은 번역을 검토하지 않고 항목과 원본만 봅니다.

### D1. GET /admin/contracts?view={보기}&q={검색어}&page=&size= — 확인 대기 목록

| `view` | 조건 | 정렬 |
| --- | --- | --- |
| `PENDING` (기본) | stage `AUTHORITY_REVIEW` | 근로자 확인이 오래된 순 |
| `APPROVED` | `authority_reviews.decision = APPROVED` | 최근 순 |
| `REJECTED` | `authority_reviews.decision = REJECTED` | 최근 순 |
| `TERMINATED` | 기관이 승인한 뒤 종료된 계약 | 최근 순 |

`q`는 계약 번호, 사업체명, 근로자 이름을 함께 찾습니다.

```json
// 응답 200
{
  "content": [
    { "contractId": 31, "contractNo": "C-2026-04817", "companyName": "한빛정밀", "workerName": "Nguyễn Văn An",
      "language": "vi", "workerConfirmedAt": "2026-11-13T05:05:00Z", "warningCount": 1, "employerInputCount": 2 }
  ],
  "page": { "number": 0, "size": 20, "totalElements": 3, "totalPages": 1 }
}
```

### D2. GET /admin/contracts/{contractId} — 계약 검토

R4의 `fields`(AI 값, 값 출처, 경고, 근거 조항)에 M2의 `employer`, `worker`, `file`, `timeline`을 더한 형식입니다. 항목별 수정 이력(`contract_term_edit_logs`)도 `editLogs: [{ key, before, after, editedAt }]`로 함께 줍니다.

### D3. POST /admin/contracts/{contractId}/approve — 확정

본문 없이 호출하며 응답은 202입니다(W2와 같은 형식). `authority_reviews`(APPROVED)를 넣고 stage를 `AUTHORITY_REVIEW → RECORDING`으로 바꿉니다. 이벤트 `AUTHORITY_APPROVED`와 고용주 알림 `AUTHORITY_REVIEWED`를 남기고 블록체인 기록을 시작합니다. 이후 과정은 W2의 `UNDERSTOOD`와 같습니다.

### D4. POST /admin/contracts/{contractId}/reject — 반려

```json
// 요청
{ "reason": "월 기본급이 최저임금에 미달합니다. 원본과 함께 다시 확인해 주세요." }
// 응답 200
{ "contractId": 31, "stage": "RETURNED" }
```

- `authority_reviews`(REJECTED, 사유 필수)를 넣고 회차를 `RETURNED`로, stage를 `RETURNED`로 바꿉니다. 이벤트 `AUTHORITY_REJECTED`와 고용주 알림 `AUTHORITY_REVIEWED`를 남깁니다. 고용주는 R8로 다시 고칩니다.
- D3과 D4 공통 에러: `409 CONTRACT_STAGE_CONFLICT` (stage가 `AUTHORITY_REVIEW`가 아님) · `409 AUTHORITY_ALREADY_REVIEWED` (`uq_authority_reviews__contract_version_id`) · `400 REJECT_REASON_REQUIRED`

## 부록

### stage 전이

아래 그림은 이 명세가 쓰는 전이입니다. 점선으로 표시한 전이(`REVIEWING → UPLOADED`)는 지금 `ContractStage.canTransitionTo`에 없어서 추가해야 합니다.

&#91;embedded content: contracts.stage 전이 · 9개 단계, 추가할 전이 1개\]

### A. 근로조건 항목 키

`fields`, `items`, `terms`, `values`, `reportItems.key`가 모두 이 키를 씁니다. 목록 순서는 `TermField` enum과 같고, 제출 시 필수 여부는 이 명세의 제안입니다.

| key | DB 컬럼 | 이름 | 타입 | 제출 시 필수 |
| --- | --- | --- | --- | --- |
| `employerName` | employer\_name | 사업체명 | STRING | 예 |
| `representativeName` | representative\_name | 대표자 | STRING | 예 |
| `employerAddress` | employer\_address | 사업장 주소 | STRING | 예 |
| `workerName` | worker\_name | 근로자 성명 | STRING | 예 |
| `workerNationality` | worker\_nationality | 국적 | STRING (ISO 2자리) | 아니오 |
| `startDate` | start\_date | 계약 시작일 | DATE | 예 |
| `endDate` | end\_date | 계약 종료일 | DATE | 아니오 |
| `workPlace` | work\_place | 근무 장소 | STRING | 예 |
| `jobDescription` | job\_description | 업무 내용 | STRING | 예 |
| `workStartTime` | work\_start\_time | 시업 시각 | TIME | 예 |
| `workEndTime` | work\_end\_time | 종업 시각 | TIME | 예 |
| `breakMinutes` | break\_minutes | 휴게시간(분) | INTEGER | 예 |
| `workDaysPerWeek` | work\_days\_per\_week | 주 근무일 수 | INTEGER | 예 |
| `workDaysText` | work\_days\_text | 근무일 원문 표현 | STRING | 아니오 |
| `weeklyHoliday` | weekly\_holiday | 주휴일 | STRING | 예 |
| `monthlyBaseWage` | monthly\_base\_wage | 월 기본급(원) | INTEGER | 예 |
| `payDay` | pay\_day | 임금 지급일 | INTEGER | 예 |
| `payMethod` | pay\_method | 지급 방법 | STRING | 예 |
| `accommodation` | accommodation | 숙식 제공 | STRING | 아니오 |

### B. 에러 코드

이미 있는 `AUTH_*`, `JWT_*`, `COMMON_*`, `MEMBER_NOT_FOUND`는 그대로 쓰고, 아래 코드를 `CustomErrorCode`에 추가합니다.

| 코드 | HTTP | 뜻 | 나오는 API |
| --- | --- | --- | --- |
| `CONTRACT_NOT_FOUND` | 404 | 계약이 없거나 볼 권한이 없음 | 계약 경로 전부 |
| `CONTRACT_STAGE_CONFLICT` | 409 | 지금 stage에서는 할 수 없는 작업 | R2\~R8, W2, M4, D3, D4 |
| `WORKER_NOT_FOUND` | 404 | 근로자 계정이 아님 | R1 |
| `PREVIOUS_CONTRACT_INVALID` | 409 | 대체할 계약이 확정 상태가 아니거나 당사자가 다름 | R1 |
| `FILE_TOO_LARGE` | 413 | 10MB 초과 | R1, R2, L2 |
| `FILE_UNSUPPORTED_TYPE` | 415 | PDF·JPG·PNG가 아님 | R1, R2 |
| `FILE_NOT_FOUND` | 404 | 계약에 속한 파일이 아님 | M3 |
| `TERM_KEY_UNKNOWN` | 400 | 없는 항목 키 | R5 |
| `TERM_VALUE_INVALID` | 400 | 타입이나 범위가 틀림 | R5 |
| `TERMS_REQUIRED_MISSING` | 422 | 필수 항목이 비어 있음 | R6 |
| `CONFIRMATION_REQUIRED` | 400 | 원본 일치 확인 체크 없음 | R6 |
| `TRANSLATION_NOT_READY` | 409 | 완료된 번역본이 없음 | W1 |
| `TRANSLATION_OUTDATED` | 409 | 응답한 번역본이 최신이 아님 | W2 |
| `READ_ALL_REQUIRED` | 400 | "모두 읽음" 체크 없이 이해함 | W2 |
| `REPORT_ITEMS_REQUIRED` | 400 | 신고 항목을 고르지 않음 | W2 |
| `TERMINATION_ALREADY_ACTIVE` | 409 | 이미 진행 중인 종료가 있음 | M4 |
| `TERMINATION_DATE_INVALID` | 400 | 종료일이 오늘 이전이거나 계약 종료일 이후 | M4 |
| `TERMINATION_NOT_ALLOWED` | 403 | 요청자가 스스로 응답하거나, 요청자가 아닌 사람이 취소 | M5, M6 |
| `TERMINATION_STATUS_CONFLICT` | 409 | 이미 응답되거나 취소된 요청 | M5, M6 |
| `AUTHORITY_ALREADY_REVIEWED` | 409 | 이 회차는 이미 검토함 | D3, D4 |
| `REJECT_REASON_REQUIRED` | 400 | 반려 사유 없음 | D4 |
| `LOOKUP_INVALID_FORMAT` | 400 | 확인번호 형식이 아님 | L1, L2 |
| `LOOKUP_NOT_FOUND` | 404 | 확정된 계약 중 그 번호가 없음 | L1, L2 |

### C. 구현 전에 정해야 할 것

1. **확인번호를 저장하는 시점 (DB 변경 필요):** 블록체인에 올리는 `anchor_key`는 keccak256(확인번호)이므로, 번호는 `RECORDING` 단계에서 이미 정해져 있어야 합니다. 그런데 `ck_contracts__confirmed_fields`는 `DONE` 전에 `confirmation_no`를 넣지 못하게 막습니다. 그래서 기록 중에 서버가 재시작되면 번호를 잃습니다. V3에서 `RECORDING`에도 `confirmation_no`를 허용하도록 CHECK를 나누는 것을 제안합니다. `confirmed_at`은 지금처럼 `DONE`과 `ENDED`에만 둡니다.
2. **stage 전이 1개 추가:** `REVIEWING → UPLOADED`(R2 원본 다시 올리기)입니다.
3. **신고 사유의 한국어 번역을 저장할 곳:** `review_responses`에는 `reason` 하나뿐이고, 이 테이블은 추가만 가능합니다. 마이그레이션 없이 `WORKER_REPORTED` 이벤트의 `payload.reasonKo`에 넣는 것을 제안합니다.
4. **번역 신고 제외에 따른 정리:** MVP에서 번역 신고를 뺐으므로, V3 마이그레이션에서 `review_report_items`의 CHECK를 `ITEM_MISMATCH`만 허용하도록 바꾸고 `field_name`을 NOT NULL로 둡니다. 코드에서는 `ReportType` enum의 `TRANSLATION`을 지웁니다.
5. **최저임금 기준 연도:** 와이어프레임은 2027년에 시작하는 계약을 2026년 최저임금(10,320원)과 비교합니다. 계약 시작일이 속한 연도의 금액을 쓰도록 연도별 설정값(`visafe.min-wage.{연도}`)을 두는 것을 제안합니다.
6. **새 계약으로 대체할 때의 순서:** `uq_contracts_one_confirmed` 때문에 같은 사업주와 근로자에게 확정 계약이 두 개 있을 수 없습니다. 새 계약을 `DONE`으로 바꾸는 트랜잭션 안에서 기존 계약을 먼저 `ENDED`(REPLACED)로 바꾸고, 기존 계약의 TERMINATE 기록은 그 뒤에 비동기로 올립니다.
7. **에러 응답에 `errorCode` 추가**와 README 수정 (공통 규칙 참고).
8. **공개 조회 설정:** `PUBLIC_PATHS`에 `/api/v1/lookup/**`를 추가하고, IP당 요청 수 제한을 둡니다. 근로자 이름 검색(A6)에 쓸 unaccent 확장도 V3 마이그레이션에서 켭니다.
9. **업로드 크기 설정:** `spring.servlet.multipart.max-file-size: 10MB`를 설정하고, `MaxUploadSizeExceededException`을 `FILE_TOO_LARGE`로 바꿉니다.
10. **진행 중인 계약 취소:** DB에 취소 상태가 없어서 이 명세에도 취소 API가 없습니다. 필요하면 stage 추가와 함께 다음 버전에서 다룹니다.

### D. 알림 API (선택)

`notifications` 테이블은 있지만 와이어프레임에 알림 화면이 없습니다. 알림은 서버가 위 흐름에서 항상 저장하고, 화면을 붙일 때 아래 3개를 씁니다.

- **N1. GET /notifications?unreadOnly=true&page=&size=:** `{ unreadCount, content: [{ notificationId, type, title, message, linkPath, contractId, readAt, createdAt }], page }`
- **N2. PATCH /notifications/{notificationId}/read:** `read_at`을 채웁니다. 응답 204
- **N3. POST /notifications/read-all:** 내 안 읽은 알림을 모두 읽음으로 바꿉니다. 응답 204
