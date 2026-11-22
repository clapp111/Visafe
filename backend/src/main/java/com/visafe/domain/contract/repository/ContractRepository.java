package com.visafe.domain.contract.repository;

import com.visafe.domain.contract.entity.Contract;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    /** 근로자 본인의 계약만 찾는다. */
    @Query("select c from Contract c where c.id = :contractId and c.worker.id = :workerMemberId")
    Optional<Contract> findForWorker(
            @Param("contractId") Long contractId, @Param("workerMemberId") Long workerMemberId);

    /** 고용주가 소속된 사업주의 계약만 찾는다. (같은 사업주의 다른 고용주가 등록한 계약 포함) */
    @Query("""
            select c from Contract c
            where c.id = :contractId
              and c.company.id = (select m.company.id from Member m where m.id = :employerMemberId)
            """)
    Optional<Contract> findForEmployer(
            @Param("contractId") Long contractId, @Param("employerMemberId") Long employerMemberId);
}
