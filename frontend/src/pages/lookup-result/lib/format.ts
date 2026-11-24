/** 트랜잭션 해시를 앞 10자·뒤 8자만 남겨 줄인다. */
export function shortenHash(hash: string) {
  return hash.length > 20 ? `${hash.slice(0, 10)}…${hash.slice(-8)}` : hash;
}
