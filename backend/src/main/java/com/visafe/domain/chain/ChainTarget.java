package com.visafe.domain.chain;

import java.util.Locale;

/** 기록과 조회에 사용할 배포 위치. */
public record ChainTarget(long chainId, String contractAddress) {

    public ChainTarget {
        if (chainId <= 0 || contractAddress == null || !contractAddress.matches("0x[0-9a-fA-F]{40}")) {
            throw new IllegalArgumentException("블록체인 네트워크 또는 컨트랙트 주소 형식이 올바르지 않습니다.");
        }
        contractAddress = contractAddress.toLowerCase(Locale.ROOT);
    }
}
