package com.visafe.domain.contract.repository;

import com.visafe.domain.contract.entity.ContractVersion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractVersionRepository extends JpaRepository<ContractVersion, Long> {}
