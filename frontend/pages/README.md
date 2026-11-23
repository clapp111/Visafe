# 이 폴더는 비워 두세요

루트에 `pages/` 폴더가 있으면 Next.js가 `src/pages/`를 Pages Router로 인식하지 않습니다. 그래서 `src/pages/`를 FSD의 pages 레이어로 쓸 수 있습니다.

라우팅은 루트 `app/`(App Router)에서 하고, 화면 구현은 `src/pages/`에 둡니다.
