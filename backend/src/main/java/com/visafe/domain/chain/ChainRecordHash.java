package com.visafe.domain.chain;

import com.visafe.domain.chain.entity.ChainRecord;
import com.visafe.global.crypto.Keccak256;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/**
 * 블록체인 확정 기록(chain_records)의 해시를 계산한다. (hash_rule_version 1)
 * <p>
 * 모든 해시는 객체 키를 알파벳순으로 둔 공백 없는 JSON을 UTF-8로 바꿔 keccak256을 구한다.
 * DB에 남은 값만으로 다시 계산할 수 있어야 하므로 한 번 기록된 규칙은 바꾸지 않는다.
 * <ul>
 *   <li>anchor_key: keccak256(확인번호)</li>
 *   <li>근로자 식별값: keccak256("근로자 회원 ID:nonce"). 근로자 ID를 그대로 올리지 않는다.</li>
 *   <li>events_hash: {@code [{"actorMemberId":7,"id":1,"occurredAt":"2026-11-13T04:24:00.123Z","type":"CONTRACT_CREATED"},…]}
 *       (id 순서, 시각은 DB에 저장된 값을 밀리초로 자름, payload는 제외)</li>
 *   <li>record_hash(CONFIRM): {@code {"anchorKey","eventsHash","fileHash","hashRuleVersion":1,"itemsHash",
 *       "recordType":"CONFIRM","translationHash","workerId"}}</li>
 * </ul>
 */
public final class ChainRecordHash {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private ChainRecordHash() {}

    /** 확인번호로 온체인 조회 키를 만든다. */
    public static String anchorKey(String confirmationNo) {
        return keccak(confirmationNo);
    }

    /** 근로자 회원 ID와 nonce로 근로자 식별값을 만든다. */
    public static String workerId(Long workerMemberId, String nonce) {
        return keccak(workerMemberId + ":" + nonce);
    }

    /** 계약 이력의 해시를 만든다. */
    public static String eventsHash(List<EventSnapshot> events) {
        return keccak(eventsJson(events));
    }

    /** 확정 기록의 묶음 해시를 만든다. */
    public static String confirmRecordHash(
            String anchorKey,
            String eventsHash,
            String fileHash,
            String itemsHash,
            String translationHash,
            String workerId) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("anchorKey", anchorKey);
        record.put("eventsHash", eventsHash);
        record.put("fileHash", fileHash);
        record.put("hashRuleVersion", ChainRecord.CURRENT_HASH_RULE_VERSION);
        record.put("itemsHash", itemsHash);
        record.put("recordType", "CONFIRM");
        record.put("translationHash", translationHash);
        record.put("workerId", workerId);
        return keccak(JSON.writeValueAsString(record));
    }

    /** events_hash 입력이 되는 JSON 문자열을 만든다. */
    static String eventsJson(List<EventSnapshot> events) {
        List<Map<String, Object>> list = new ArrayList<>();
        events.stream().sorted(Comparator.comparing(EventSnapshot::id)).forEach(event -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("actorMemberId", event.actorMemberId());
            entry.put("id", event.id());
            entry.put(
                    "occurredAt",
                    event.occurredAt().truncatedTo(ChronoUnit.MILLIS).toString());
            entry.put("type", event.type().name());
            list.add(entry);
        });
        return JSON.writeValueAsString(list);
    }

    private static String keccak(String text) {
        return Keccak256.hash(text.getBytes(StandardCharsets.UTF_8));
    }
}
