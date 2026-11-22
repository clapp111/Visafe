package com.visafe.domain.chain.repository;

import com.visafe.domain.chain.entity.ChainRecord;
import com.visafe.domain.chain.enums.RecordType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChainRecordRepository extends JpaRepository<ChainRecord, Long> {

    /** 회차의 기록 (확정 기록은 회차당 1개, uq_chain_records_confirm_version). */
    Optional<ChainRecord> findFirstByContractVersionIdAndRecordTypeOrderByIdDesc(
            Long contractVersionId, RecordType recordType);
}
