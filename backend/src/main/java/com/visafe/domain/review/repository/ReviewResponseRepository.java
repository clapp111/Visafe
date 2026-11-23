package com.visafe.domain.review.repository;

import com.visafe.domain.review.entity.ReviewResponse;
import com.visafe.domain.review.enums.ReviewAction;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewResponseRepository extends JpaRepository<ReviewResponse, Long> {

    /** 회차의 마지막 응답 중 주어진 종류. */
    Optional<ReviewResponse> findFirstByTranslationContractVersionIdAndActionOrderByIdDesc(
            Long contractVersionId, ReviewAction action);
}
