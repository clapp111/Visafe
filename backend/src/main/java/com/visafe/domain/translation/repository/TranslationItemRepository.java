package com.visafe.domain.translation.repository;

import com.visafe.domain.translation.entity.TranslationItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TranslationItemRepository extends JpaRepository<TranslationItem, Long> {}
