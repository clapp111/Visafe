package com.visafe.domain.contract.entity;

import com.visafe.domain.contract.enums.ContractEventType;
import com.visafe.domain.member.entity.Member;
import com.visafe.global.common.BaseCreatedTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * F. 계약 이력 (contract_events) — 추가 전용. DB 트리거가 수정·삭제를 막는다.
 * 진행 상황·확인 이력·기관 검토 이력 화면이 이 로그를 (created_at, id) 순으로 보여 주며,
 * 블록체인 기록 시 events_hash 계산의 입력이 된다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Immutable
@Entity
@Table(name = "contract_events")
public class ContractEvent extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    /** 회차 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_version_id")
    private ContractVersion contractVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private ContractEventType eventType;

    /** 행동한 사람 (시스템은 null) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_member_id")
    private Member actor;

    /** 상세 내용 (JSON 객체) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false)
    private Map<String, Object> payload = new LinkedHashMap<>();

    private ContractEvent(
            Contract contract,
            ContractVersion contractVersion,
            ContractEventType eventType,
            Member actor,
            Map<String, Object> payload) {
        this.contract = contract;
        this.contractVersion = contractVersion;
        this.eventType = eventType;
        this.actor = actor;
        this.payload = payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload);
    }

    public static ContractEvent of(
            Contract contract,
            ContractVersion contractVersion,
            ContractEventType eventType,
            Member actor,
            Map<String, Object> payload) {
        return new ContractEvent(contract, contractVersion, eventType, actor, payload);
    }

    /** 시스템이 남기는 이벤트 (actor 없음). */
    public static ContractEvent bySystem(
            Contract contract,
            ContractVersion contractVersion,
            ContractEventType eventType,
            Map<String, Object> payload) {
        return new ContractEvent(contract, contractVersion, eventType, null, payload);
    }
}
