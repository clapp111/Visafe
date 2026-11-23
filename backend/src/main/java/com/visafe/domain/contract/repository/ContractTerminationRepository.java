package com.visafe.domain.contract.repository;

import com.visafe.domain.contract.entity.ContractTermination;
import com.visafe.domain.contract.enums.TerminationStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContractTerminationRepository extends JpaRepository<ContractTermination, Long> {

    /** 계약의 마지막 종료 기록 중 주어진 상태인 것. */
    Optional<ContractTermination> findFirstByContractIdAndStatusOrderByIdDesc(
            Long contractId, TerminationStatus status);

    /** 계약의 진행 중인 종료 (REQUESTED·AGREED는 계약당 하나, uq_contract_terminations_active). */
    Optional<ContractTermination> findFirstByContractIdAndStatusInOrderByIdDesc(
            Long contractId, Collection<TerminationStatus> statuses);

    /** 종료일이 지났는데 아직 확정 상태인 계약의 합의 종료 ID. */
    @Query("""
            select t.id from ContractTermination t
            where t.reasonType = com.visafe.domain.contract.enums.TerminationReasonType.MUTUAL
              and t.status = com.visafe.domain.contract.enums.TerminationStatus.AGREED
              and t.endDate < :today
              and t.contract.stage = com.visafe.domain.contract.enums.ContractStage.DONE
            order by t.id
            """)
    List<Long> findDueMutualIds(@Param("today") LocalDate today);
}
