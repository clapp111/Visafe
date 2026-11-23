# Visafe 프론트엔드

Node.js 22.18 이상이 필요합니다. `frontend/`에서 실행합니다.

```powershell
npm ci
npm run dev
```

[개발 서버](http://localhost:3000)에서 첫 화면을 확인할 수 있습니다. 개발 서버는 `/api/*` 요청을 `http://localhost:8080`의 같은 경로로 전달합니다. 백엔드 주소를 바꾸려면 `.env.local`에 `API_PROXY_TARGET`을 설정합니다.

## API 타입 생성

백엔드를 실행한 뒤 `npm run generate:api`를 실행하면 `http://localhost:8080/v3/api-docs`에서 `src/shared/api/schema.d.ts`를 생성합니다. 생성된 타입 파일을 커밋합니다. 빌드는 이 타입 파일을 사용하므로 백엔드 없이 실행할 수 있습니다.

## 검증

```powershell
npm run lint
npm run lint:fsd
npm run format:check
npm run build
```
