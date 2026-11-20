package com.visafe.global.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class Keccak256Test {

    @Test
    void emptyInputMatchesEthereumKeccak() {
        // SHA3-256("")은 0xa7ffc6f8…로 다르다. 이더리움 keccak256과 같아야 온체인 값과 비교할 수 있다.
        assertThat(Keccak256.hash(new byte[0]))
                .isEqualTo("0xc5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470");
    }

    @Test
    void knownVector() {
        assertThat(Keccak256.hash("hello".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo("0x1c8aff950685c2ed4bc3174f3472287b56d9517b9c948127319a09a7a36deac8");
    }

    @Test
    void resultMatchesDatabaseHashFormat() {
        assertThat(Keccak256.hash("계약서".getBytes(StandardCharsets.UTF_8))).matches("^0x[0-9a-f]{64}$");
    }
}
