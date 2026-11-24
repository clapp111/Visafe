/** 원본 계약서로 받는 형식 (서버 R1·R2와 같게 둔다) */
export const CONTRACT_FILE_ACCEPT = "application/pdf,image/jpeg,image/png";

const ACCEPTED_NAME = /\.(pdf|jpe?g|png)$/i;
const MAX_BYTES = 10 * 1024 * 1024;

/** 원본 계약서로 올릴 수 없는 파일이면 그 이유(형식·크기)를 돌려준다. */
export function checkContractFile(file: File): "type" | "size" | undefined {
  if (!ACCEPTED_NAME.test(file.name)) return "type";
  if (file.size > MAX_BYTES) return "size";
  return undefined;
}
