package com.visafe.domain.chain.recorder;

import com.visafe.domain.chain.enums.RecordType;
import java.util.Optional;

/**
 * 로컬 스텁 기록을 저장하고 조회한다. 실제 Sepolia 기록은 {@link ChainTransactionRecorder}와 {@link ChainRecordScheduler}가 처리한다.
 */
public interface ChainRecorder {

    /**
     * 조회 키와 기록 종류에 묶음 해시를 기록하고 블록에 포함될 때까지 기다린다.
     * 확정 기록과 종료 기록은 같은 조회 키를 쓰므로 기록 종류로 구분한다.
     *
     * @param anchorKey 조회 키 (keccak256(확인번호))
     * @param recordType 기록 종류
     * @param recordHash 묶음 해시
     * @return 트랜잭션 해시와 블록 번호. 기록하지 못하면 예외를 던진다
     */
    ChainReceipt record(String anchorKey, RecordType recordType, String recordHash);

    /**
     * 조회 키와 기록 종류에 기록된 묶음 해시를 읽는다. (공개 조회의 무결성 확인)
     *
     * @param anchorKey 조회 키 (keccak256(확인번호))
     * @param recordType 기록 종류
     * @return 온체인에 기록된 묶음 해시, 기록이 없으면 빈 값. 체인을 읽지 못하면 예외를 던진다
     */
    Optional<String> readRecordHash(String anchorKey, RecordType recordType);
}
