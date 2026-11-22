package com.visafe.domain.contract.repository;

import com.visafe.domain.chain.EventSnapshot;
import com.visafe.domain.contract.entity.ContractEvent;
import com.visafe.domain.contract.enums.ContractEventType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContractEventRepository extends JpaRepository<ContractEvent, Long> {

    /** 회차의 이벤트 중 주어진 종류만 일어난 순서대로 찾는다. */
    @Query("""
            select e from ContractEvent e
            where e.contractVersion.id = :versionId and e.eventType in :eventTypes
            order by e.createdAt, e.id
            """)
    List<ContractEvent> findVersionEvents(
            @Param("versionId") Long versionId, @Param("eventTypes") Collection<ContractEventType> eventTypes);

    /** 계약의 모든 이벤트를 id 순서로 DB 저장값 그대로 읽는다. (events_hash 계산용) */
    @Query("""
            select new com.visafe.domain.chain.EventSnapshot(e.id, e.eventType, a.id, e.createdAt)
            from ContractEvent e left join e.actor a
            where e.contract.id = :contractId
            order by e.id
            """)
    List<EventSnapshot> findSnapshotsByContractId(@Param("contractId") Long contractId);
}
