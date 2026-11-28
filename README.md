# Visafe

외국인 근로자의 근로계약서를 등록하고 AI로 추출·번역해 근로자가 모국어로 확인할 수 있게 하는 서비스입니다. 확정·종료된 계약의 해시를 Sepolia 블록체인에 기록해, 공개 조회에서 원본 파일과 대조할 수 있습니다.

사용자 역할은 사업주(`EMPLOYER`), 근로자(`WORKER`), 감독기관(`ADMIN`) 세 가지입니다.

## 저장소 구조

| 경로 | 내용 | 문서 |
| --- | --- | --- |
| `frontend/` | Next.js 16 · React 19 · TanStack Query · Tailwind CSS 4 (FSD 구조) | [frontend/README.md](frontend/README.md) |
| `backend/` | Spring Boot 4.1 · Java 21 · PostgreSQL 17 · Flyway | [backend/README.md](backend/README.md) |
| `contracts/` | 계약 해시 기록용 스마트 컨트랙트 (Solidity · Hardhat · Ignition) | [contracts/README.md](contracts/README.md) |
| `docs/` | 브랜치·커밋 규칙, Javadoc 규칙, API 초안 | |

## 구성

```
브라우저
  └─ frontend (Next.js, :3000)
       └─ /api/* ─▶ backend (Spring Boot, :8080)
                      ├─ PostgreSQL        계약·회원 데이터
                      ├─ S3                계약서 원본 (Presigned URL로 내려받기)
                      ├─ LLM               계약서 추출·번역·질문 답변
                      └─ Sepolia           ContractAnchorRegistry에 확정·종료 해시 기록
```

## 로컬 실행

필요한 도구: JDK 21, Node.js 22.18 이상, Docker

### 1. 백엔드

```powershell
cd backend
Copy-Item .env.example .env   # 값 채우기
docker compose up -d          # PostgreSQL
.\gradlew.bat bootRun         # http://localhost:8080
```

- `S3_BUCKET`이 없으면 서버가 시작되지 않습니다. S3 설정은 [backend/README.md](backend/README.md#파일-저장소-s3)를 참고하세요.
- `LLM_API_KEY`가 없으면 계약서 추출·번역이 동작하지 않습니다.
- Sepolia 기록은 기본으로 꺼져 있습니다(`SEPOLIA_RECORDING_ENABLED`).

### 2. 프론트엔드

```powershell
cd frontend
npm ci
npm run dev                   # http://localhost:3000
```

### 3. 로그인

local 프로필에는 데모 계정이 들어 있습니다. 계정 목록과 비밀번호는 [backend/README.md](backend/README.md#데모-계정-localdev)에 있습니다.

| 주소 | 설명 |
| --- | --- |
| http://localhost:3000 | 프론트엔드 |
| http://localhost:8080/swagger-ui.html | Swagger UI |
| http://localhost:8080/actuator/health | 헬스 체크 |

## 개발 흐름

1. `dev`에서 작업 브랜치를 만듭니다. (`feat/{이슈번호}-{요약}`)
2. 작업 후 `dev`로 PR을 올리고, CI가 통과하면 병합합니다.
3. 배포할 때 `dev` → `main`으로 PR을 올립니다.

브랜치·커밋·PR 규칙은 [docs/branch-commit-convention.md](docs/branch-commit-convention.md)를 따릅니다.

### CI

| 워크플로 | 실행 조건 |
| --- | --- |
| Backend CI | `backend/` 변경 |
| Frontend CI | `frontend/` 변경 |
| Contracts | `contracts/` 변경, Sepolia 배포는 수동 실행 |
| PR Labeler | PR 생성 시 변경 경로에 맞춰 라벨 추가 |

## 문서

- 브랜치·커밋 규칙: [docs/branch-commit-convention.md](docs/branch-commit-convention.md)
- Javadoc 규칙: [docs/javadoc-convention.md](docs/javadoc-convention.md)
