package com.visafe.domain.chain.recorder;

import com.visafe.global.crypto.Keccak256;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * 실제 블록체인 연동 전까지 쓰는 기록기. 네트워크에 보내지 않고 바로 블록에 포함된 것으로 돌려준다.
 * 트랜잭션 해시는 묶음 해시로 만든 가짜 값이라 기록마다 다르다. (chain_records.tx_hash 유일)
 */
@Component
public class StubChainRecorder implements ChainRecorder {

    private final AtomicLong blockNumber = new AtomicLong(1);

    @Override
    public ChainReceipt record(String anchorKey, String recordHash) {
        String txHash = Keccak256.hash(("stub-tx:" + recordHash).getBytes(StandardCharsets.UTF_8));
        return new ChainReceipt(txHash, blockNumber.getAndIncrement());
    }
}
