package com.visafe.domain.contract.enums;

import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 올릴 수 있는 계약서 원본 형식. 브라우저가 보낸 Content-Type은 바꿀 수 있으므로 파일 앞 바이트(매직 넘버)로 판별한다.
 */
@RequiredArgsConstructor
public enum ContractFileType {
    PDF("application/pdf", "pdf", new byte[] {'%', 'P', 'D', 'F', '-'}),
    PNG("image/png", "png", new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}),
    JPEG("image/jpeg", "jpg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

    @Getter
    private final String mimeType;

    @Getter
    private final String extension;

    @SuppressWarnings("ImmutableEnumChecker") // 바깥에 내주지 않고 비교에만 쓰므로 바뀌지 않는다
    private final byte[] signature;

    public static Optional<ContractFileType> detect(byte[] content) {
        return Arrays.stream(values()).filter(type -> type.matches(content)).findFirst();
    }

    private boolean matches(byte[] content) {
        if (content.length < signature.length) {
            return false;
        }
        return Arrays.equals(content, 0, signature.length, signature, 0, signature.length);
    }
}
