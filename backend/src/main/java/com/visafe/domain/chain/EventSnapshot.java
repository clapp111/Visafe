package com.visafe.domain.chain;

import com.visafe.domain.contract.enums.ContractEventType;
import java.time.Instant;

/**
 * events_hash에 넣는 이벤트 값. 영속성 컨텍스트가 아니라 DB에 저장된 값으로 만든다. (시각 정밀도가 DB 기준이어야 다시 계산할 수 있음)
 *
 * @param id 이벤트 ID
 * @param type 이벤트 종류
 * @param actorMemberId 행동한 회원 ID, 서버가 한 일이면 null
 * @param occurredAt 일어난 시각 (contract_events.created_at)
 */
public record EventSnapshot(Long id, ContractEventType type, Long actorMemberId, Instant occurredAt) {}
