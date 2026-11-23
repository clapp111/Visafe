import fsd from "@feature-sliced/steiger-plugin";
import { defineConfig } from "steiger";

export default defineConfig([
  ...fsd.configs.recommended,
  {
    // #53 헤더와 연결하기 전에 독립적으로 검증하기 위한 예외다.
    files: ["./src/features/change-language/**"],
    rules: { "fsd/insignificant-slice": "off" },
  },
]);
