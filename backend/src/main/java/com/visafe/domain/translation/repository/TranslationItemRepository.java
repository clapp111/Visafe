package com.visafe.domain.translation.repository;

import com.visafe.domain.translation.entity.TranslationItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TranslationItemRepository extends JpaRepository<TranslationItem, Long> {

    List<TranslationItem> findByTranslationId(Long translationId);
}
