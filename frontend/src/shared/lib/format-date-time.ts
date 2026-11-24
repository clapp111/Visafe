// 계약 관련 시각은 한국 시간으로 보인다 (계약이 한국에서 맺어지므로 보는 사람의 시간대와 관계없이 같게)
const DATE_TIME = new Intl.DateTimeFormat("ko-KR", {
  timeZone: "Asia/Seoul",
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
  hourCycle: "h23",
});

/** `2026-11-13T05:06:00Z` → `2026.11.13. 14:06` */
export function formatDateTime(dateTime: string) {
  const parts = Object.fromEntries(
    DATE_TIME.formatToParts(new Date(dateTime)).map((part) => [
      part.type,
      part.value,
    ]),
  );
  return `${parts.year}.${parts.month}.${parts.day}. ${parts.hour}:${parts.minute}`;
}
