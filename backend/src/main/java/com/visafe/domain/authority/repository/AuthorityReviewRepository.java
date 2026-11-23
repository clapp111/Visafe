package com.visafe.domain.authority.repository;

import com.visafe.domain.authority.entity.AuthorityReview;
import com.visafe.domain.authority.enums.ReviewDecision;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthorityReviewRepository extends JpaRepository<AuthorityReview, Long> {

    /** 회차의 검토 (회차당 하나, uq_authority_reviews__contract_version_id). */
    Optional<AuthorityReview> findByContractVersionId(Long contractVersionId);

    boolean existsByContractVersionId(Long contractVersionId);

    /** 주어진 결정의 검토 전체 (계약·사업주·근로자 포함). */
    @Query("""
            select r from AuthorityReview r
            join fetch r.contractVersion v join fetch v.contract c join fetch c.company join fetch c.worker
            where r.decision = :decision
            """)
    List<AuthorityReview> findAllWithContractByDecision(@Param("decision") ReviewDecision decision);
}
