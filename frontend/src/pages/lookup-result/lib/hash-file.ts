import { keccak_256 } from "@noble/hashes/sha3.js";
import { bytesToHex } from "@noble/hashes/utils.js";

export async function hashFile(file: File) {
  const bytes = new Uint8Array(await file.arrayBuffer());
  return `0x${bytesToHex(keccak_256(bytes))}`;
}
