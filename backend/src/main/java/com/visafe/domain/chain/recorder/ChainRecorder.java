package com.visafe.domain.chain.recorder;

import java.util.Optional;

/**
 * 묶음 해시를 블록체인에 기록한다. 실제 Sepolia 연동 전까지는 {@link StubChainRecorder}를 쓴다.
 */
public interface ChainRecorder {

    /**
     * 조회 키에 묶음 해시를 기록하고 블록에 포함될 때까지 기다린다.
     *
     * @param anchorKey 조회 키 (keccak256(확인번호))
     * @param recordHash 묶음 해시
     * @return 트랜잭션 해시와 블록 번호. 기록하지 못하면 예외를 던진다
     */
    ChainReceipt record(String anchorKey, String recordHash);

    /**
     * 조회 키에 기록된 묶음 해시를 읽는다. (공개 조회의 무결성 확인)
     *
     * @param anchorKey 조회 키 (keccak256(확인번호))
     * @return 온체인에 기록된 묶음 해시, 기록이 없으면 빈 값. 체인을 읽지 못하면 예외를 던진다
     */
    Optional<String> readRecordHash(String anchorKey);
}
