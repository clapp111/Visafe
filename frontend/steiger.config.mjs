import fsd from "@feature-sliced/steiger-plugin";
import { defineConfig } from "steiger";

export default defineConfig([
  ...fsd.configs.recommended,
  {
    // 계약 화면에서 공통 M2 폴링을 사용하기 전까지 적용한다.
    files: ["./src/entities/contract/**"],
    rules: { "fsd/insignificant-slice": "off" },
  },
  {
    // 계약 상세(#63)·근로자 확인(#64)·기관 검토(#70)에서도 쓰기 전까지 적용한다.
    files: ["./src/widgets/contract-file-viewer/**"],
    rules: { "fsd/insignificant-slice": "off" },
  },
]);
