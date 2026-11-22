package com.visafe.domain.translation.repository;

import com.visafe.domain.translation.entity.TranslationClause;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TranslationClauseRepository extends JpaRepository<TranslationClause, Long> {

    List<TranslationClause> findByTranslationId(Long translationId);
}
