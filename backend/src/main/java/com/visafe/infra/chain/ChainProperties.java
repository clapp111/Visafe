package com.visafe.infra.chain;

import com.visafe.domain.chain.ChainTarget;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 블록체인 기록 설정 (application.yml의 visafe.chain). 기록을 켰을 때만 {@link Web3jChainRecorder}가 값을 검사한다.
 *
 * @param rpcUrl Sepolia RPC 주소
 * @param contractAddress 배포한 기록 컨트랙트 주소
 * @param writerPrivateKey 기록 지갑 비밀키
 * @param deploymentBlock 컨트랙트를 배포한 블록. 기존 기록 이벤트를 이 블록부터 찾는다
 */
@ConfigurationProperties("visafe.chain")
public record ChainProperties(String rpcUrl, String contractAddress, String writerPrivateKey, long deploymentBlock) {

    private static final long SEPOLIA_CHAIN_ID = 11155111L;

    /** Sepolia 모드에서 새 확정 기록에 고정할 배포 위치. */
    public ChainTarget sepoliaTarget() {
        return new ChainTarget(SEPOLIA_CHAIN_ID, contractAddress);
    }
}
