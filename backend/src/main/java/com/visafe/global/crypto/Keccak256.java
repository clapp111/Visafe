package com.visafe.global.crypto;

import java.util.HexFormat;
import org.bouncycastle.jcajce.provider.digest.Keccak;

/**
 * keccak256 해시 (이더리움과 같은 방식, SHA3-256과 다름).
 * 파일 해시(contract_files.file_hash), 항목 해시, 블록체인 기록 키에 쓴다.
 * 결과는 DB 형식과 같은 0x + 소문자 16진수 64자리다.
 */
public final class Keccak256 {

    private static final HexFormat HEX = HexFormat.of();

    private Keccak256() {}

    /** 바이트의 keccak256을 0x + 소문자 16진수 64자리로 돌려준다. */
    public static String hash(byte[] input) {
        // Digest 객체는 스레드 안전하지 않아 호출마다 새로 만든다.
        byte[] digest = new Keccak.Digest256().digest(input);
        return "0x" + HEX.formatHex(digest);
    }
}
