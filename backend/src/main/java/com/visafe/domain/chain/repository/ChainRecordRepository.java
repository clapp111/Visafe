package com.visafe.domain.chain.repository;

import com.visafe.domain.chain.entity.ChainRecord;
import com.visafe.domain.chain.enums.RecordType;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChainRecordRepository extends JpaRepository<ChainRecord, Long> {

    /** 계약의 모든 기록 (만든 순서). */
    List<ChainRecord> findByContractVersionContractIdOrderByIdAsc(Long contractId);

    /** 회차의 기록 (확정 기록은 회차당 1개, uq_chain_records_confirm_version). */
    Optional<ChainRecord> findFirstByContractVersionIdAndRecordTypeOrderByIdDesc(
            Long contractVersionId, RecordType recordType);

    /** 이미 서명한 하나의 트랜잭션을 다른 nonce보다 먼저 처리한다. */
    Optional<ChainRecord> findFirstByNetworkAndSignedTransactionIsNotNull(String network);

    @Query(value = """
            SELECT * FROM chain_records
            WHERE network = 'sepolia' AND status = 'PENDING'
              AND (next_attempt_at IS NULL OR next_attempt_at <= :now)
            ORDER BY id LIMIT 1
            """, nativeQuery = true)
    Optional<ChainRecord> findNextSepoliaRecord(@Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select record from ChainRecord record where record.id = :id")
    Optional<ChainRecord> findByIdForUpdate(@Param("id") Long id);
}
