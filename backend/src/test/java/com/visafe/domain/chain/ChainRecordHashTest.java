package com.visafe.domain.chain;

import static org.assertj.core.api.Assertions.assertThat;

import com.visafe.domain.contract.enums.ContractEventType;
import com.visafe.global.crypto.Keccak256;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChainRecordHashTest {

    private static final String HASH_A = "0x" + "a".repeat(64);
    private static final String HASH_B = "0x" + "b".repeat(64);

    @Test
    void eventsJsonIsOrderedByIdWithMillisecondTimes() {
        List<EventSnapshot> events = List.of(
                new EventSnapshot(
                        12L, ContractEventType.WORKER_UNDERSTOOD, 9L, Instant.parse("2026-11-13T04:30:00.123456Z")),
                new EventSnapshot(10L, ContractEventType.SUBMITTED, 7L, Instant.parse("2026-11-13T04:24:00Z")),
                new EventSnapshot(
                        11L, ContractEventType.TRANSLATION_COMPLETED, null, Instant.parse("2026-11-13T04:29:59.5Z")));

        assertThat(ChainRecordHash.eventsJson(events))
                .isEqualTo(
                        "[{\"actorMemberId\":7,\"id\":10,\"occurredAt\":\"2026-11-13T04:24:00Z\",\"type\":\"SUBMITTED\"},"
                                + "{\"actorMemberId\":null,\"id\":11,\"occurredAt\":\"2026-11-13T04:29:59.500Z\",\"type\":\"TRANSLATION_COMPLETED\"},"
                                + "{\"actorMemberId\":9,\"id\":12,\"occurredAt\":\"2026-11-13T04:30:00.123Z\",\"type\":\"WORKER_UNDERSTOOD\"}]");
        assertThat(ChainRecordHash.eventsHash(events)).isEqualTo(keccak(ChainRecordHash.eventsJson(events)));
    }

    @Test
    void confirmRecordHashBindsAllComponentsInAlphabeticalOrder() {
        String json = "{\"anchorKey\":\"" + HASH_A + "\",\"eventsHash\":\"" + HASH_B + "\",\"fileHash\":\"" + HASH_A
                + "\",\"hashRuleVersion\":1,\"itemsHash\":\"" + HASH_B + "\",\"recordType\":\"CONFIRM\","
                + "\"translationHash\":\"" + HASH_A + "\",\"workerId\":\"" + HASH_B + "\"}";

        assertThat(ChainRecordHash.confirmRecordHash(HASH_A, HASH_B, HASH_A, HASH_B, HASH_A, HASH_B))
                .isEqualTo(keccak(json));
    }

    @Test
    void anchorKeyAndWorkerIdAreKeccakOfText() {
        assertThat(ChainRecordHash.anchorKey("VS-7K3F-9QXM")).isEqualTo(keccak("VS-7K3F-9QXM"));
        assertThat(ChainRecordHash.workerId(5L, HASH_A)).isEqualTo(keccak("5:" + HASH_A));
    }

    private static String keccak(String text) {
        return Keccak256.hash(text.getBytes(StandardCharsets.UTF_8));
    }
}
