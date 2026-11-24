/** `2027-01-02` → `2027.01.02.` */
export function formatDate(date: string) {
  return `${date.replaceAll("-", ".")}.`;
}

/** 계약기간. 종료일이 없으면 시작일만 보인다. */
export function formatPeriod(startDate: string, endDate?: string | null) {
  return endDate
    ? `${formatDate(startDate)} ~ ${formatDate(endDate)}`
    : `${formatDate(startDate)} ~`;
}

/** 트랜잭션 해시를 앞 10자·뒤 8자만 남겨 줄인다. */
export function shortenHash(hash: string) {
  return hash.length > 20 ? `${hash.slice(0, 10)}…${hash.slice(-8)}` : hash;
}

/** 계약 시작일부터 종료일까지의 길이를 반올림한 개월 수로 센다. */
export function countContractMonths(startDate: string, endDate: string) {
  const days = (Date.parse(endDate) - Date.parse(startDate)) / 86_400_000;
  return Math.round(days / (365.25 / 12));
}
