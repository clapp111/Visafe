package com.visafe.domain.contract.repository;

import com.visafe.domain.contract.entity.ContractTermination;
import com.visafe.domain.contract.enums.TerminationStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractTerminationRepository extends JpaRepository<ContractTermination, Long> {

    /** 계약의 마지막 종료 기록 중 주어진 상태인 것. */
    Optional<ContractTermination> findFirstByContractIdAndStatusOrderByIdDesc(
            Long contractId, TerminationStatus status);
}
