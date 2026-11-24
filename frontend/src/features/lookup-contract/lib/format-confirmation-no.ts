/**
 * 입력한 확인번호를 `VS-XXXX-XXXX` 모양으로 맞춘다.
 * 영문·숫자만 남겨 대문자로 바꾸고, 2·4·4자리로 끊어 하이픈을 넣는다. 문자 범위는 서버가 확인한다.
 */
export function formatConfirmationNo(input: string) {
  const chars = input
    .toUpperCase()
    .replace(/[^0-9A-Z]/g, "")
    .slice(0, 10);
  return [chars.slice(0, 2), chars.slice(2, 6), chars.slice(6)]
    .filter(Boolean)
    .join("-");
}
