import type { components } from "@/shared/api";

type FileCheckResponse = components["schemas"]["FileCheckResponse"];

const RECORDED_FILE_HASH =
  "0x8f3a2c71e4b5d6a7c8f9e0d1b2a3c4d5e6f7a8b9c0d1e2f3a4b5c6d9c3e7c21d";

// 화면 확인용 와이어프레임 예시값. L2 파일 대조를 연결하면 지운다.
const MATCH: FileCheckResponse = {
  match: true,
  uploadedFileHash: RECORDED_FILE_HASH,
  recordedFileHash: RECORDED_FILE_HASH,
};

const MISMATCH: FileCheckResponse = {
  match: false,
  uploadedFileHash:
    "0x51d0e9b4c2a1f87d3e6b90a4c5d2e1f0b3a7c8d9e0f1a2b3c4d5e6fa72f0e3b8",
  recordedFileHash: RECORDED_FILE_HASH,
};

/**
 * 고른 파일에 맞는 예시 결과를 고른다. 와이어프레임처럼 이름에 '수정'이 들어간 파일은 불일치, 나머지는 일치로 다룬다.
 */
export function getFileCheckFixture(file: File): FileCheckResponse {
  return file.name.includes("수정") ? MISMATCH : MATCH;
}
