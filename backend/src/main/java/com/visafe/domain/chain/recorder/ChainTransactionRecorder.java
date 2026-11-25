package com.visafe.domain.chain.recorder;

import com.visafe.domain.chain.ChainTarget;
import com.visafe.domain.chain.enums.RecordType;
import java.util.Optional;

/**
 * 기록 트랜잭션을 서명·전송·확인하는 블록체인 연동. 단계를 나눠 서버가 서명본을 저장하고, 재시작한 뒤에도 같은 트랜잭션으로 이어 간다.
 * 구현은 infra에 있고 설정으로 켰을 때만 빈이 생긴다. 로컬 스텁 기록은 {@link ChainRecorder}가 맡는다.
 */
public interface ChainTransactionRecorder {

    /** 새 기록에 저장할 배포 위치. */
    ChainTarget configuredTarget();

    /** 같은 해시가 이미 기록되어 있으면 처음 기록한 트랜잭션의 영수증. 다른 해시가 있으면 {@link Rejected}. */
    Optional<ChainReceipt> findExisting(Write write);

    /** 기록 트랜잭션에 서명해 서명본(0x 16진수)을 돌려준다. */
    String prepare(Write write);

    /** 채굴되지 않은 서명본을 같은 nonce와 더 높은 수수료로 다시 서명한다. */
    String replace(Write write, String signedTransaction);

    /** 서명본의 트랜잭션 해시. */
    String transactionHash(String signedTransaction);

    /** 서명본을 보낸다. 이미 받은 트랜잭션이면 성공으로 본다. 이 서명본으로 더는 기록할 수 없으면 {@link Dropped}. */
    void broadcast(ChainTarget target, String signedTransaction);

    Optional<ChainReceipt> receipt(Write write, String txHash);

    Optional<String> readRecordHash(ChainTarget target, String anchorKey, RecordType recordType);

    record Write(ChainTarget target, String anchorKey, RecordType recordType, String recordHash) {}

    /** 온체인 값 충돌·확정된 revert처럼 다시 보내도 완료할 수 없는 결과. */
    final class Rejected extends RuntimeException {
        public Rejected(String message) {
            super(message);
        }
    }

    /** 같은 nonce가 이미 쓰였거나 수수료가 부족해 이 서명본으로는 기록할 수 없다. 새로 서명해야 한다. */
    final class Dropped extends RuntimeException {
        public Dropped(String message) {
            super(message);
        }
    }
}
