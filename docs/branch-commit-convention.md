# Visafe 작업 규칙

## 작업 흐름

1. `dev`에서 작업 브랜치를 만듭니다.
2. 작업 후 `dev`로 PR을 올립니다. CI(backend 변경 시 `Backend CI`)가 통과해야 머지합니다.
3. 배포할 때 `dev` → `main`으로 PR을 올립니다.

```bash
git switch dev
git pull
git switch -c feat/contract-upload
```

## 유형

브랜치 이름과 커밋 메시지는 같은 유형을 씁니다.

| 유형 | 쓰는 경우 | 예 |
| --- | --- | --- |
| `feat` | 새 기능 | 계약 업로드 API, 근로자 검색 |
| `fix` | 버그 수정 | 확인번호 중복 발급 수정 |
| `chore` | 기능 변화 없는 정리·설정·의존성 | 패키지 정리, 라이브러리 버전 올림, 빌드 설정 |
| `perf` | 성능 개선 | N+1 쿼리 제거, 인덱스 추가 |
| `security` | 보안 관련 | 인증·권한 검사, 개인정보 암호화, 취약 의존성 교체 |
| `deploy` | 배포·CI/CD·인프라 | GitHub Actions, Docker, 서버 설정 |
| `docs` | 문서만 바꿈 | README, 설계 문서 |

## 브랜치 이름

`<유형>/<짧은-설명>` — 영어 소문자와 하이픈(`-`)만 씁니다.

```
feat/contract-upload
fix/confirmation-no-duplicate
chore/flyway-setup
perf/contract-list-query
security/alien-reg-no-encryption
deploy/backend-ci
docs/erd-update
```

이슈를 쓰면 번호를 붙여도 됩니다: `feat/12-contract-upload`

## 영역

한 저장소에 세 프로젝트가 있어서, 커밋과 PR 제목에 어느 영역의 변경인지 적습니다.

| 영역 | 폴더 | 커밋·PR 제목 앞 표기 | PR 라벨 |
| --- | --- | --- | --- |
| 프론트엔드 | `frontend/` | 🔵 | `frontend` |
| 백엔드 | `backend/` | 🔴 | `backend` |
| 스마트 컨트랙트 | `contracts/` | 🟣 | `contracts` |

- 여러 영역을 함께 바꾸면 영역별로 커밋을 나눕니다. 나누기 어려운 공통 변경(루트 설정, `docs/`, `.github/`)은 이모지를 붙이지 않습니다.
- PR 라벨은 바뀐 폴더를 보고 자동으로 붙습니다. (`.github/labeler.yml`) 여러 영역을 바꾸면 라벨도 여러 개 붙습니다.
- 이모지 입력: Windows `Win + .`, macOS `Ctrl + Cmd + Space`에서 "파란 원"·"빨간 원"·"보라 원"을 검색하거나 위 표에서 복사합니다.

## 커밋 메시지

`<이모지> <유형>: <무엇을 했는지>` — 한국어로 쓰고, 마침표는 붙이지 않습니다.

```
🔴 feat: 계약서 원본 업로드 API 추가
🔵 fix: 확인번호 입력칸 자동 대문자 변환
🟣 chore: Hardhat 설정 추가
docs: 브랜치·커밋 규칙에 영역 표기 추가
```

변경이 여러 개면 한 줄 띄우고 본문에 목록으로 적습니다.

```
🔴 feat: 계약서 원본 업로드 API 추가

- 파일 해시(keccak256) 계산 후 contract_files에 저장
- PDF·이미지만 허용, 최대 20MB
```

## PR

- 제목은 커밋 메시지와 같은 형식: `🔴 feat: 계약서 원본 업로드 API 추가`
- 대상 브랜치는 `dev` (배포 때만 `main`)
- 영역 라벨은 자동으로 붙으니 직접 달지 않아도 됩니다.
