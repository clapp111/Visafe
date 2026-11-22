package com.visafe.domain.contract.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ContractFileTypeTest {

    @Test
    void detectsByLeadingBytes() {
        assertThat(ContractFileType.detect("%PDF-1.7\n...".getBytes(StandardCharsets.US_ASCII)))
                .contains(ContractFileType.PDF);
        assertThat(ContractFileType.detect(new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0}))
                .contains(ContractFileType.PNG);
        assertThat(ContractFileType.detect(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}))
                .contains(ContractFileType.JPEG);
    }

    @Test
    void rejectsOtherFormatsAndTooShortFiles() {
        assertThat(ContractFileType.detect("hello".getBytes(StandardCharsets.UTF_8)))
                .isEmpty();
        assertThat(ContractFileType.detect(new byte[] {'%', 'P'})).isEmpty();
        assertThat(ContractFileType.detect(new byte[0])).isEmpty();
    }
}
