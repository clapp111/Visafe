package com.visafe.domain.term.repository;

import com.visafe.domain.term.entity.ContractTermEditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractTermEditLogRepository extends JpaRepository<ContractTermEditLog, Long> {

    /** 회차의 항목 수정 이력 (수정한 순서). */
    List<ContractTermEditLog> findByContractVersionIdOrderByIdAsc(Long contractVersionId);
}
