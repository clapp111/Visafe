package com.visafe.domain.term.repository;

import com.visafe.domain.term.entity.ContractTermAnnotation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContractTermAnnotationRepository extends JpaRepository<ContractTermAnnotation, Long> {

    /** 회차의 항목 부가 정보를 근거 조항과 함께 찾는다. */
    @Query(
            "select a from ContractTermAnnotation a left join fetch a.contractClause where a.contractVersion.id = :versionId")
    List<ContractTermAnnotation> findWithClauseByContractVersionId(@Param("versionId") Long versionId);

    /** 회차의 항목 부가 정보를 모두 지운다. (원본을 다시 올려 다시 추출할 때) */
    @Modifying(flushAutomatically = true)
    @Query("delete from ContractTermAnnotation a where a.contractVersion.id = :versionId")
    void deleteByContractVersionId(@Param("versionId") Long versionId);
}
