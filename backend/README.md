# Visafe Backend

Visafe API 서버 (Spring Boot 4.1 / Java 21 / PostgreSQL)

브랜치·커밋·PR 규칙은 [docs/branch-commit-convention.md](../docs/branch-commit-convention.md)를 따릅니다.

## 기술 스택

| 구분 | 사용 기술 |
| --- | --- |
| Language | Java 21 (JDK 21 이상으로 빌드) |
| Framework | Spring Boot 4.1.2 (Web MVC, Validation, Data JPA, Actuator) |
| DB | PostgreSQL 17 (로컬: Docker Compose), 테스트: H2 (PostgreSQL 모드) |
| 마이그레이션 | Flyway (`src/main/resources/db/migration`) |
| API 문서 | springdoc-openapi (Swagger UI) |
| Build | Gradle 9 (Kotlin DSL, Wrapper 포함) |

## 로컬 실행

```bash
# 1. PostgreSQL 실행
docker compose up -d

# 2. 서버 실행 (기본 프로필: local)
./gradlew bootRun
```

Windows PowerShell에서는 `./gradlew` 대신 `.\gradlew.bat`를 사용합니다.

| URL | 설명 |
| --- | --- |
| http://localhost:8080/swagger-ui.html | Swagger UI |
| http://localhost:8080/v3/api-docs | OpenAPI 스펙 (JSON) |
| http://localhost:8080/actuator/health | 헬스 체크 |

## 테스트 / 빌드

```bash
./gradlew test    # H2 인메모리 DB로 테스트 (Docker 불필요)
./gradlew build   # build/libs/backend-0.0.1-SNAPSHOT.jar 생성
```

실제 PostgreSQL 스키마(CHECK 제약, 트리거, 생성 컬럼)가 필요한 테스트는 `IT_DB_URL`이 있을 때만 실행됩니다. Flyway가 V1부터 데모 시드까지 적용하고, 각 테스트는 롤백되어 데이터가 남지 않습니다.

```bash
docker compose up -d
# cleanTest: 환경 변수만 바꾸면 Gradle이 이전 결과(건너뜀)를 재사용하므로 다시 실행하게 한다
IT_DB_URL=jdbc:postgresql://localhost:5432/visafe ./gradlew cleanTest test   # IT_DB_USERNAME·IT_DB_PASSWORD 기본값 visafe
```

## 코드 포맷 / 정적 분석

| 도구 | 역할 | 실행 시점 |
| --- | --- | --- |
| [Spotless](https://github.com/diffplug/spotless) + Palantir Java Format | 포맷 검사·자동 정렬, 안 쓰는 import 제거, 줄 끝 공백·마지막 줄바꿈 정리 (LF) | `./gradlew build`(`check`)에 포함, pre-commit 훅 |
| [Error Prone](https://errorprone.info/) | 컴파일 시 버그 패턴 정적 분석 (오류는 빌드 실패, 경고는 출력만) | 모든 `compileJava`, pre-push 훅 |

```bash
./gradlew spotlessApply   # 포맷 자동 정리
./gradlew spotlessCheck   # 포맷 검사만
./gradlew compileJava     # Error Prone 분석
```

- 새 프로젝트라 변경 파일만이 아니라 **전체 Java 파일**을 검사합니다.
- 꺼 둔 Error Prone 검사: `MissingOverride`(전체), 테스트 코드의 `UnusedVariable`·`JavaTimeDefaultTimeZone`. 의도한 코드에는 `@SuppressWarnings("검사이름")`과 이유 주석을 답니다.
- IntelliJ: palantir-java-format 플러그인을 설치하고 **Settings → Other Settings → palantir-java-format → Enable**을 켜면 IDE 포맷(Ctrl+Alt+L)이 Spotless와 같아집니다.

### Git 훅 설치 (클론 후 1회)

모노레포라 `.git`은 상위 `Visafe/`에 있습니다. backend 폴더에서 실행하면 `scripts/hooks`의 훅이 `Visafe/.git/hooks`로 복사됩니다.

```bash
./gradlew installGitHook
```

| 훅 | 동작 |
| --- | --- |
| pre-commit | 커밋에 `backend/src`의 Java 파일이 있으면 `spotlessCheck` |
| pre-push | push할 커밋에 `backend/` 변경이 있으면 `compileJava compileTestJava` (Error Prone) |

frontend·contracts만 바꾼 커밋/푸시는 검사 없이 통과합니다. 훅 스크립트를 고치면 `installGitHook`을 다시 실행하세요.

## 프로필 & 환경 변수

`src/main/resources`의 `application.yml`에 공통 설정을 두고, 환경마다 다른 값만 프로필 파일에 둡니다.
프로필은 `SPRING_PROFILES_ACTIVE`로 고르며, 지정하지 않으면 `local`입니다.

| 파일 | 용도 |
| --- | --- |
| `application.yml` | 공통 (Flyway, JPA `ddl-auto: validate`, `open-in-view: false`, UTC, Actuator, Swagger 경로 등) |
| `application-local.yml` | 로컬 개발. `backend/.env` 자동 로드, DB 기본값은 compose의 PostgreSQL, SQL 로그 |
| `application-dev.yml` | 개발 서버. DB 접속 정보는 환경 변수 필수, 커넥션 풀 5 |
| `application-prod.yml` | 운영 서버. DB 접속 정보는 환경 변수 필수, 커넥션 풀 10, Swagger 비활성화 |
| `src/test/resources/application-test.yml` | 테스트. H2 인메모리 DB, Flyway 끔, `ddl-auto: create-drop` |

| 환경 변수 | local 기본값 | dev / prod |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/visafe` | 필수 |
| `DB_USERNAME` / `DB_PASSWORD` | `visafe` / `visafe` | 필수 |
| `SERVER_PORT` | `8080` | `8080` |
| `POSTGRES_PORT` | `5432` (docker compose 호스트 포트) | — |

로컬 값은 `.env.example`을 복사해 `backend/.env`로 만들어 바꿉니다. `.env`는 Spring(local 프로필)과 `docker compose`가 함께 읽으며 커밋하지 않습니다.
PC에 PostgreSQL이 설치되어 5432를 이미 쓰고 있다면 `.env`에 `POSTGRES_PORT=5433`과 `DB_URL=jdbc:postgresql://localhost:5433/visafe`를 넣으세요.

## DB 마이그레이션 (Flyway)

스키마는 `src/main/resources/db/migration`의 SQL로만 만들고 바꿉니다. 서버가 시작할 때 Flyway가 아직 적용하지 않은 파일을 버전 순서대로 실행하고,
이어서 Hibernate가 엔티티와 스키마가 맞는지 검사(`ddl-auto: validate`)합니다. 맞지 않으면 서버가 뜨지 않습니다.

| 파일 | 내용 |
| --- | --- |
| `V1__init.sql` | 초기 스키마 (테이블 22개, FK·CHECK·인덱스·트리거). 설계 문서: Visafe ERD 설계 |

**스키마를 바꿀 때**
1. 새 파일을 추가합니다: `V2__add_xxx_to_contracts.sql` (`V{번호}__{설명}.sql`, 밑줄 2개)
2. 엔티티를 함께 고칩니다. 서버를 띄우면 validate가 둘이 맞는지 확인합니다.
3. **이미 적용된 파일(`V1__init.sql` 등)은 고치지 않습니다.** 내용이 바뀌면 Flyway가 체크섬 불일치로 시작을 막습니다.

로컬 DB를 처음부터 다시 만들려면 볼륨을 지우고 다시 띄웁니다. (로컬 데이터가 모두 지워집니다)

```bash
docker compose down -v
docker compose up -d
```

### 데모 계정 (local·dev)

local·dev 프로필은 `db/seed/R__demo_seed.sql`로 데모 계정을 넣습니다. prod에는 들어가지 않습니다. 비밀번호는 모두 `visafe-demo-1234`입니다.

| 이메일 | 역할 | 이름 | 비고 |
| --- | --- | --- | --- |
| `employer@demo.visafe.test` | EMPLOYER | 김민수 | 사업주 한빛정밀 |
| `worker1@demo.visafe.test` | WORKER | Nguyễn Văn An | 1998-04-17, 외국인등록번호 뒷자리 4821 |
| `worker2@demo.visafe.test` | WORKER | Nguyễn Văn An | 동명이인. 2001-09-03, 1357 |
| `worker3@demo.visafe.test` | WORKER | Lê Thị Hoa | 1996-12-25, 2468 |
| `admin@demo.visafe.test` | ADMIN | 감독기관 담당자 | |

## 파일 저장소 (S3)

계약서 원본은 S3에 저장하고, 브라우저는 서버가 발급한 Presigned URL(5분)로 S3에서 직접 받습니다. 저장한 바이트는 바꾸지 않습니다. 내려받은 원본의 keccak256이 블록체인 기록과 같아야 공개 조회의 파일 대조가 맞기 때문입니다.

버킷 하나를 환경별 키 접두사로 나눠 씁니다. 로컬에서 올린 파일도 실제 S3에 저장됩니다.

| 환경 | 키 접두사 | 접근 방법 |
| --- | --- | --- |
| local | `local/` | `.env`의 개인 IAM 사용자 키 (`S3_ACCESS_KEY`·`S3_SECRET_KEY`, 비우면 `~/.aws` 자격 증명) |
| dev | `dev/` | 서버 IAM Role |
| prod | `prod/` | 서버 IAM Role |

객체 키는 `{접두사}/contracts/{계약 ID}/{UUID}.{확장자}` 형식이며, 원래 파일 이름은 키에 넣지 않고 DB에만 저장합니다.

### 로컬 설정

`backend/.env`에 `S3_BUCKET`, `S3_ACCESS_KEY`, `S3_SECRET_KEY`를 넣습니다. `S3_BUCKET`이 없으면 서버가 시작되지 않습니다. 접근 키는 `local/*`만 다룰 수 있는 개인 IAM 사용자로 발급합니다. (아래 IAM 정책에서 `Resource`를 `arn:aws:s3:::<버킷 이름>/local/*`로)

저장소 통합 테스트(`S3FileStorageIntegrationTest`)는 실제 S3로 저장 → 읽기 → Presigned URL 내려받기 → 삭제를 돌려 봅니다. `S3_IT_BUCKET`이 있을 때만 실행되며(CI에서는 건너뜀), `local/it-test/` 아래에 썼다가 지웁니다.

```bash
# .env와 같은 값
S3_IT_BUCKET=<버킷> S3_IT_ACCESS_KEY=<키> S3_IT_SECRET_KEY=<비밀 키> ./gradlew test --tests '*S3FileStorageIntegrationTest'
```

### 서버 환경 변수

| 변수 | 설명 |
| --- | --- |
| `S3_BUCKET` | 버킷 이름 (dev·prod 필수) |
| `S3_REGION` | 기본 `ap-northeast-2` |
| `S3_OBJECT_LOCK_ENABLED` | prod에서 `true`로 두면 올리는 파일마다 Governance 모드 3년 보존을 붙입니다. 버킷에 객체 잠금이 켜져 있어야 하고, 꺼진 버킷에서 켜면 업로드가 실패합니다 |

접근 키는 넣지 않고 서버의 IAM Role로 접근합니다.

### 버킷 설정

- 모든 퍼블릭 액세스 차단, 버전 관리, 기본 암호화(SSE-S3)를 켭니다.
- 객체 잠금은 켜되 **기본 보존 기간은 두지 않습니다.** 기본 보존은 버킷 전체에 걸려 dev 파일까지 지울 수 없게 됩니다. 보존은 서버가 prod 파일에만 붙입니다.
- CORS (권한 → CORS). pdf.js가 Range 요청으로 읽으므로 관련 헤더를 노출합니다. 프론트 주소는 실제 값으로 바꿉니다.

```json
[
  {
    "AllowedMethods": ["GET", "HEAD"],
    "AllowedOrigins": ["https://<프론트 dev 주소>", "https://<프론트 prod 주소>", "http://localhost:5173"],
    "AllowedHeaders": ["*"],
    "ExposedHeaders": ["Content-Disposition", "Content-Length", "Content-Type", "Content-Range", "Accept-Ranges"],
    "MaxAgeSeconds": 3000
  }
]
```

### 서버 IAM 정책

dev 서버는 `dev/`만, prod 서버는 `prod/`만 다룹니다. prod는 보존을 우회하거나 버전을 지우는 권한(`s3:BypassGovernanceRetention`, `s3:DeleteObjectVersion`)을 주지 않습니다. `s3:DeleteObject`는 DB 저장이 롤백된 파일 정리용이며, 버전 관리 버킷에서는 삭제 마커만 남기고 원본 버전은 보존됩니다.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "VisafeProdObjects",
      "Effect": "Allow",
      "Action": ["s3:PutObject", "s3:PutObjectRetention", "s3:GetObject", "s3:DeleteObject"],
      "Resource": "arn:aws:s3:::<버킷 이름>/prod/*"
    }
  ]
}
```

dev 서버 Role과 로컬 개발용 IAM 사용자는 같은 형식에서 `Resource`를 각각 `arn:aws:s3:::<버킷 이름>/dev/*`, `arn:aws:s3:::<버킷 이름>/local/*`로 바꾸고 `s3:PutObjectRetention`을 뺍니다.

## 패키지 구조

```
com.visafe
├── VisafeApplication.java
├── global
│   ├── common     # BaseTimeEntity 등 공통 클래스
│   ├── config     # Security, CORS, OpenAPI 설정
│   ├── exception  # CustomErrorCode, RestApiException, GlobalExceptionHandler
│   └── jwt        # JWT 발급·검증 필터, 리프레시 토큰
└── domain         # 도메인별 패키지 (예: domain/member/{controller,service,repository,entity,dto})
```

## 에러 응답 형식

모든 에러는 아래 형식으로 응답합니다. 컨트롤러 예외는 `GlobalExceptionHandler`가, 인증 필터·Security 예외는 `ErrorResponseWriter`가 같은 형식으로 씁니다.

```json
{ "code": 409, "errorCode": "CONTRACT_STAGE_CONFLICT", "message": "지금 단계에서는 할 수 없는 작업입니다." }
```

| 필드 | 설명 |
| --- | --- |
| `code` | HTTP 상태 코드 |
| `errorCode` | `CustomErrorCode` 상수 이름. 같은 HTTP 상태 안에서 원인을 구분할 때 씁니다 |
| `message` | 화면에 보여 줄 수 있는 메시지 |

비즈니스 예외는 `CustomErrorCode`에 코드를 추가하고 `throw new RestApiException(CustomErrorCode.XXX)`로 발생시킵니다. 기본 메시지 대신 구체적인 내용을 담으려면 `new RestApiException(CustomErrorCode.XXX, "메시지")`를 씁니다.
