package com.visafe.domain.review.repository;

import com.visafe.domain.review.entity.ReviewQuestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewQuestionRepository extends JpaRepository<ReviewQuestion, Long> {

    /** 회차에서 회원이 한 질문 (질문한 순서). */
    List<ReviewQuestion> findByContractVersionIdAndAskedByIdOrderByIdAsc(Long contractVersionId, Long askedById);
}
