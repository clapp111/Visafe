package com.visafe.domain.contract.repository;

import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.enums.ContractStage;
import java.time.LocalDate;
import java.util.List;
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

    /** 근로자 본인의 계약 전체 (목록용, 사업주·현재 회차 포함). */
    @Query("""
            select c from Contract c join fetch c.company left join fetch c.currentVersion
            where c.worker.id = :workerMemberId
            """)
    List<Contract> findAllForWorker(@Param("workerMemberId") Long workerMemberId);

    /** 고용주가 소속된 사업주의 계약 전체 (목록용, 근로자·현재 회차 포함). */
    @Query("""
            select c from Contract c join fetch c.worker left join fetch c.currentVersion
            where c.company.id = (select m.company.id from Member m where m.id = :employerMemberId)
            """)
    List<Contract> findAllForEmployer(@Param("employerMemberId") Long employerMemberId);

    /** 주어진 stage의 계약 전체 (기관 목록용, 사업주·근로자·현재 회차 포함). */
    @Query("""
            select c from Contract c join fetch c.company join fetch c.worker left join fetch c.currentVersion
            where c.stage = :stage
            """)
    List<Contract> findAllWithPartiesByStage(@Param("stage") ContractStage stage);

    /** 계약 번호 일련번호 (V4 contract_no_seq). */
    @Query(value = "SELECT nextval('contract_no_seq')", nativeQuery = true)
    long nextContractNoSequence();

    /** 이 계약을 대체한 새 계약이 이미 있는지 (uq_contracts_previous). */
    boolean existsByPreviousContractId(Long previousContractId);

    boolean existsByConfirmationNo(String confirmationNo);

    Optional<Contract> findByConfirmationNo(String confirmationNo);

    /** 계약 종료일(현재 회차의 항목 값)이 지났는데 아직 확정 상태인 계약 ID. */
    @Query("""
            select c.id from Contract c, ContractLaborTerms t
            where t.contractVersion = c.currentVersion
              and c.stage = com.visafe.domain.contract.enums.ContractStage.DONE
              and t.endDate < :today
            order by c.id
            """)
    List<Long> findExpiredIds(@Param("today") LocalDate today);
}
