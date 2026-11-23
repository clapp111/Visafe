package com.visafe.domain.review.controller;

import com.visafe.domain.contract.dto.response.ContractStageResponse;
import com.visafe.domain.member.entity.MemberDetails;
import com.visafe.domain.review.dto.request.QuestionCreateRequest;
import com.visafe.domain.review.dto.request.ReviewResponseCreateRequest;
import com.visafe.domain.review.dto.response.ContractReviewResponse;
import com.visafe.domain.review.dto.response.QuestionResponse;
import com.visafe.domain.review.service.ContractQuestionService;
import com.visafe.domain.review.service.ContractReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Review", description = "근로자 계약 확인·응답·질문")
@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
public class ReviewController {

    private final ContractReviewService contractReviewService;
    private final ContractQuestionService contractQuestionService;

    @Operation(
            summary = "확인할 계약 내용 조회",
            description =
                    "현재 회차의 마지막 완료 번역본을 기준으로 항목 번역·쉬운 설명, 원문·번역 조항, 꼭 확인할 항목, 경고를 조회한다. stage가 AWAITING_WORKER가 아니면 canRespond가 false다.")
    @PreAuthorize("hasRole('WORKER')")
    @GetMapping("/{contractId}/review")
    public ContractReviewResponse getReview(
            @PathVariable Long contractId, @AuthenticationPrincipal MemberDetails member) {
        return contractReviewService.getReview(contractId, member);
    }

    @Operation(
            summary = "이해함 · 원본과 다름 응답",
            description =
                    "UNDERSTOOD면 확정 기록을 시작하고(기관 검토가 필요하면 AUTHORITY_REVIEW), REPORTED면 계약을 고용주에게 돌려보낸다. translationId는 W1에서 받은 값을 보낸다.")
    @PreAuthorize("hasRole('WORKER')")
    @PostMapping("/{contractId}/review-responses")
    public ResponseEntity<ContractStageResponse> respond(
            @PathVariable Long contractId,
            @Valid @RequestBody ReviewResponseCreateRequest request,
            @AuthenticationPrincipal MemberDetails member) {
        return ResponseEntity.accepted().body(contractReviewService.respond(contractId, request, member));
    }

    @Operation(summary = "질문 목록", description = "현재 회차에서 내가 한 질문을 오래된 순으로 답변·근거 조항과 함께 조회한다.")
    @PreAuthorize("hasRole('WORKER')")
    @GetMapping("/{contractId}/questions")
    public List<QuestionResponse> listQuestions(
            @PathVariable Long contractId, @AuthenticationPrincipal MemberDetails member) {
        return contractQuestionService.list(contractId, member);
    }

    @Operation(
            summary = "질문하기",
            description = "이 계약서 조항만 근거로 AI가 바로 답한다(최대 20초). 실패하거나 시간이 초과되면 질문만 저장하고 answer가 null이다.")
    @PreAuthorize("hasRole('WORKER')")
    @PostMapping("/{contractId}/questions")
    public ResponseEntity<QuestionResponse> ask(
            @PathVariable Long contractId,
            @Valid @RequestBody QuestionCreateRequest request,
            @AuthenticationPrincipal MemberDetails member) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contractQuestionService.ask(contractId, request, member));
    }
}
