package com.visafe.domain.translation.repository;

import com.visafe.domain.translation.entity.Translation;
import com.visafe.domain.translation.enums.TranslationStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TranslationRepository extends JpaRepository<Translation, Long> {

    /** 회차의 마지막 번역본 (translation_no가 가장 큰 것). */
    Optional<Translation> findFirstByContractVersionIdOrderByTranslationNoDesc(Long contractVersionId);

    /** 회차의 마지막 번역본 중 주어진 상태인 것. */
    Optional<Translation> findFirstByContractVersionIdAndStatusOrderByTranslationNoDesc(
            Long contractVersionId, TranslationStatus status);
}
