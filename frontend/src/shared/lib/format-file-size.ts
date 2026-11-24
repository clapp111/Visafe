/** 파일 크기. 1MB 미만은 KB, 그 이상은 소수 첫째 자리까지 MB로 보인다. */
export function formatFileSize(bytes: number) {
  return bytes < 1024 * 1024
    ? `${Math.max(1, Math.round(bytes / 1024))}KB`
    : `${(bytes / 1024 / 1024).toFixed(1)}MB`;
}
