package com.visafe.domain.review.dto.response;

import com.visafe.domain.review.entity.ReviewQuestion;
import com.visafe.domain.review.entity.ReviewQuestionCitation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** 질문과 답변 (W3 목록의 항목, W4 응답). */
@Schema(description = "질문 응답 DTO")
public record QuestionResponse(
        @Schema(description = "질문 ID", example = "9") Long questionId,
        @Schema(description = "질문 언어", example = "vi") String language,

        @Schema(description = "질문", example = "야근하면 돈을 더 받나요?")
        String question,

        @Schema(description = "답변, AI가 실패하거나 시간이 초과되면 null", nullable = true)
        String answer,

        @Schema(description = "상담센터(1350) 안내 여부", example = "true")
        boolean referredToCenter,

        @Schema(description = "답변 근거 조항") List<Citation> citations,

        @Schema(description = "질문 일시", example = "2026-11-13T04:40:00Z")
        Instant askedAt,

        @Schema(description = "답변 일시", example = "2026-11-13T04:40:06Z", nullable = true)
        Instant answeredAt) {

    public static QuestionResponse of(ReviewQuestion question, List<ReviewQuestionCitation> citations) {
        return new QuestionResponse(
                question.getId(),
                question.getLanguage(),
                question.getQuestion(),
                question.getAnswer(),
                question.isReferredToCenter(),
                citations.stream()
                        .map(citation -> new Citation(
                                citation.getContractClause().getId(),
                                citation.getContractClause().getClauseNo(),
                                citation.getContractClause().getPageNo(),
                                citation.getQuotedText()))
                        .toList(),
                question.getCreatedAt(),
                question.getAnsweredAt());
    }

    @Schema(description = "근거 조항")
    public record Citation(
            @Schema(description = "조항 ID", example = "215") Long clauseId,

            @Schema(description = "조항 번호", example = "제8조", nullable = true)
            String clauseNo,

            @Schema(description = "쪽", example = "2", nullable = true)
            Integer pageNo,

            @Schema(description = "인용 문장", nullable = true) String quotedText) {}
}
