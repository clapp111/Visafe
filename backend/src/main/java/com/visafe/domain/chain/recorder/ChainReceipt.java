package com.visafe.domain.chain.recorder;

/**
 * 블록에 포함된 기록.
 *
 * @param txHash 트랜잭션 해시 (0x + 소문자 16진수 64자리)
 * @param blockNumber 블록 번호
 */
public record ChainReceipt(String txHash, long blockNumber) {}
