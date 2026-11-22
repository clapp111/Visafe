package com.visafe.domain.contract.repository;

import com.visafe.domain.contract.entity.ContractClause;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractClauseRepository extends JpaRepository<ContractClause, Long> {

    /** 원본 파일의 조항 (읽는 순서대로). */
    List<ContractClause> findByContractFileIdOrderBySeqAsc(Long contractFileId);
}
