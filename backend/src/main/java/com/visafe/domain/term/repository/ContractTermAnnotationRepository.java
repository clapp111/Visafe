package com.visafe.domain.term.repository;

import com.visafe.domain.term.entity.ContractTermAnnotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContractTermAnnotationRepository extends JpaRepository<ContractTermAnnotation, Long> {

    /** 회차의 항목 부가 정보를 모두 지운다. (원본을 다시 올려 다시 추출할 때) */
    @Modifying(flushAutomatically = true)
    @Query("delete from ContractTermAnnotation a where a.contractVersion.id = :versionId")
    void deleteByContractVersionId(@Param("versionId") Long versionId);
}
