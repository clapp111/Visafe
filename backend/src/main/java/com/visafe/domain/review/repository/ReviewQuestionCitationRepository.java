package com.visafe.domain.review.repository;

import com.visafe.domain.review.entity.ReviewQuestionCitation;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewQuestionCitationRepository extends JpaRepository<ReviewQuestionCitation, Long> {

    /** 질문들의 근거 조항 (인용 조항 포함, 저장한 순서). */
    @Query("""
            select c from ReviewQuestionCitation c join fetch c.contractClause
            where c.reviewQuestion.id in :questionIds
            order by c.id
            """)
    List<ReviewQuestionCitation> findWithClauseByQuestionIds(@Param("questionIds") Collection<Long> questionIds);
}
