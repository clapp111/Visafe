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

## 커밋 메시지

`<유형>: <무엇을 했는지>` — 한국어로 쓰고, 마침표는 붙이지 않습니다.

```
feat: 계약서 원본 업로드 API 추가
fix: 확정 시 확인번호가 중복 발급되는 문제 수정
```

변경이 여러 개면 한 줄 띄우고 본문에 목록으로 적습니다.

```
feat: 계약서 원본 업로드 API 추가

- 파일 해시(keccak256) 계산 후 contract_files에 저장
- PDF·이미지만 허용, 최대 20MB
```

## PR

- 제목은 커밋 메시지와 같은 형식: `feat: 계약서 원본 업로드 API 추가`
- 대상 브랜치는 `dev` (배포 때만 `main`)
