# Visafe Backend

Visafe API 서버 (Spring Boot 4.1 / Java 21 / PostgreSQL)

## 기술 스택

| 구분 | 사용 기술 |
| --- | --- |
| Language | Java 21 (JDK 21 이상으로 빌드) |
| Framework | Spring Boot 4.1.2 (Web MVC, Validation, Data JPA, Actuator) |
| DB | PostgreSQL 17 (로컬: Docker Compose), 테스트: H2 (PostgreSQL 모드) |
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
| `application.yml` | 공통 (JPA `ddl-auto: validate`, `open-in-view: false`, UTC, Actuator, Swagger 경로 등) |
| `application-local.yml` | 로컬 개발. `backend/.env` 자동 로드, DB 기본값은 compose의 PostgreSQL, `ddl-auto: update`(Flyway 도입 전까지), SQL 로그 |
| `application-dev.yml` | 개발 서버. DB 접속 정보는 환경 변수 필수, 커넥션 풀 5 |
| `application-prod.yml` | 운영 서버. DB 접속 정보는 환경 변수 필수, 커넥션 풀 10, Swagger 비활성화 |
| `src/test/resources/application-test.yml` | 테스트. H2 인메모리 DB, `ddl-auto: create-drop` |

| 환경 변수 | local 기본값 | dev / prod |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/visafe` | 필수 |
| `DB_USERNAME` / `DB_PASSWORD` | `visafe` / `visafe` | 필수 |
| `SERVER_PORT` | `8080` | `8080` |
| `POSTGRES_PORT` | `5432` (docker compose 호스트 포트) | — |

로컬 값은 `.env.example`을 복사해 `backend/.env`로 만들어 바꿉니다. `.env`는 Spring(local 프로필)과 `docker compose`가 함께 읽으며 커밋하지 않습니다.
PC에 PostgreSQL이 설치되어 5432를 이미 쓰고 있다면 `.env`에 `POSTGRES_PORT=5433`과 `DB_URL=jdbc:postgresql://localhost:5433/visafe`를 넣으세요.

## 패키지 구조

```
com.visafe
├── VisafeApplication.java
├── global
│   ├── common   # BaseTimeEntity 등 공통 클래스
│   ├── config   # JPA Auditing, OpenAPI 설정
│   └── error    # ErrorCode, BusinessException, GlobalExceptionHandler
└── domain       # 도메인별 패키지 (예: domain/user/{controller,service,repository,entity,dto})
```

## 에러 응답 형식

[RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457) 형식에 `code` 속성을 추가해 반환합니다.

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "잘못된 입력값입니다.",
  "instance": "/api/v1/...",
  "code": "C001",
  "errors": [{ "field": "name", "message": "공백일 수 없습니다" }]
}
```

비즈니스 예외는 `ErrorCode`에 코드를 추가하고 `throw new BusinessException(ErrorCode.XXX)`로 발생시킵니다.
