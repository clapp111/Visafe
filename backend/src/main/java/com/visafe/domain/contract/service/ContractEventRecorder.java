package com.visafe.domain.contract.service;

import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.entity.ContractEvent;
import com.visafe.domain.contract.enums.ContractEventType;
import com.visafe.domain.contract.repository.ContractEventRepository;
import com.visafe.domain.member.entity.Member;
import com.visafe.domain.member.repository.MemberRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractEventRecorder {

    private final ContractEventRepository contractEventRepository;
    private final MemberRepository memberRepository;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 회원이 한 일을 계약 이력에 남긴다.
     *
     * <p>이벤트는 계약의 현재 회차에 붙는다. 이력은 추가만 할 수 있고(DB 트리거), 블록체인 기록의 events_hash 입력이 되므로
     * 호출하는 쪽의 트랜잭션 안에서 상태 변경과 함께 저장한다.</p>
     *
     * @param contract 이벤트가 일어난 계약
     * @param eventType 이벤트 종류
     * @param actorMemberId 행동한 회원 ID
     * @param payload 상세 내용, 없으면 빈 객체로 저장
     * @return 저장한 이벤트
     */
    @Transactional
    public ContractEvent record(
            Contract contract, ContractEventType eventType, Long actorMemberId, Map<String, Object> payload) {
        Member actor = memberRepository.getReferenceById(actorMemberId);
        return contractEventRepository.save(
                ContractEvent.of(contract, contract.getCurrentVersion(), eventType, actor, payload));
    }

    /**
     * 서버가 한 일(AI 추출, 번역, 블록체인 기록 등)을 계약 이력에 남긴다.
     *
     * @param contract 이벤트가 일어난 계약
     * @param eventType 이벤트 종류
     * @param payload 상세 내용, 없으면 빈 객체로 저장
     * @return 저장한 이벤트
     */
    @Transactional
    public ContractEvent recordBySystem(Contract contract, ContractEventType eventType, Map<String, Object> payload) {
        return contractEventRepository.save(
                ContractEvent.bySystem(contract, contract.getCurrentVersion(), eventType, payload));
    }
}
