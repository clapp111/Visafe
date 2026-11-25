package com.visafe.infra.chain;

/**
 * 실제 블록체인 기록을 켜는 조건식. 켜면 Web3j 구현과 기록 스케줄러를 빈으로 등록한다.
 * <p>
 * {@code @ConditionalOnExpression}에 그대로 쓴다. (예: {@code @ConditionalOnExpression(ChainCondition.ENABLED)})
 */
public final class ChainCondition {

    /** SEPOLIA_RECORDING_ENABLED=true일 때 */
    public static final String ENABLED = "'${visafe.chain.sepolia-enabled:false}'.trim() == 'true'";

    private ChainCondition() {}
}
