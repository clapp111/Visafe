package com.visafe.domain.contract.repository;

import com.visafe.domain.contract.entity.ContractFile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractFileRepository extends JpaRepository<ContractFile, Long> {

    /** 계약에 속한 파일만 찾는다. (다른 계약의 파일 ID로는 찾을 수 없음) */
    Optional<ContractFile> findByIdAndContractId(Long id, Long contractId);
}
