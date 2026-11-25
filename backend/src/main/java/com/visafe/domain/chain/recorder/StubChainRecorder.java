package com.visafe.domain.chain.recorder;

import com.visafe.domain.chain.enums.RecordType;
import com.visafe.global.crypto.Keccak256;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * 로컬과 테스트용 기록기. 기록을 메모리에 추가하고 바로 영수증을 돌려준다.
 * 트랜잭션 해시는 묶음 해시로 만든 가짜 값이라 기록마다 다르다. (chain_records.tx_hash 유일)
 * <p>
 * 기록한 값은 메모리에만 두므로, 서버를 다시 시작한 뒤 읽으면 기록이 바뀌었다고 오해하지 않도록 체인을 읽지 못한 것(예외)으로 본다.
 */
@Component
public class StubChainRecorder implements ChainRecorder {

    private final AtomicLong blockNumber = new AtomicLong(1);
    private final Map<String, String> recordHashes = new ConcurrentHashMap<>();

    @Override
    public ChainReceipt record(String anchorKey, RecordType recordType, String recordHash) {
        String txHash = Keccak256.hash(("stub-tx:" + recordHash).getBytes(StandardCharsets.UTF_8));
        recordHashes.put(anchorKey + ":" + recordType, recordHash);
        return new ChainReceipt(txHash, blockNumber.getAndIncrement());
    }

    @Override
    public Optional<String> readRecordHash(String anchorKey, RecordType recordType) {
        String recordHash = recordHashes.get(anchorKey + ":" + recordType);
        if (recordHash == null) {
            throw new IllegalStateException("stub 기록기 메모리에 없는 기록입니다. (서버 재시작 등)");
        }
        return Optional.of(recordHash);
    }
}
