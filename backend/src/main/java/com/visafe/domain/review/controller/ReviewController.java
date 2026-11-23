package com.visafe.domain.review.controller;

import com.visafe.domain.contract.dto.response.ContractStageResponse;
import com.visafe.domain.member.entity.MemberDetails;
import com.visafe.domain.review.dto.request.QuestionCreateRequest;
import com.visafe.domain.review.dto.request.ReviewResponseCreateRequest;
import com.visafe.domain.review.dto.response.ContractReviewResponse;
import com.visafe.domain.review.dto.response.QuestionResponse;
import com.visafe.domain.review.service.ContractQuestionService;
import com.visafe.domain.review.service.ContractReviewService;
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

@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
public class ReviewController implements ReviewControllerDocument {

    private final ContractReviewService contractReviewService;
    private final ContractQuestionService contractQuestionService;

    @Override
    @PreAuthorize("hasRole('WORKER')")
    @GetMapping("/{contractId}/review")
    public ResponseEntity<ContractReviewResponse> getReview(
            @PathVariable Long contractId, @AuthenticationPrincipal MemberDetails member) {
        return ResponseEntity.ok(contractReviewService.getReview(contractId, member));
    }

    @Override
    @PreAuthorize("hasRole('WORKER')")
    @PostMapping("/{contractId}/review-responses")
    public ResponseEntity<ContractStageResponse> respond(
            @PathVariable Long contractId,
            @Valid @RequestBody ReviewResponseCreateRequest request,
            @AuthenticationPrincipal MemberDetails member) {
        return ResponseEntity.accepted().body(contractReviewService.respond(contractId, request, member));
    }

    @Override
    @PreAuthorize("hasRole('WORKER')")
    @GetMapping("/{contractId}/questions")
    public ResponseEntity<List<QuestionResponse>> listQuestions(
            @PathVariable Long contractId, @AuthenticationPrincipal MemberDetails member) {
        return ResponseEntity.ok(contractQuestionService.list(contractId, member));
    }

    @Override
    @PreAuthorize("hasRole('WORKER')")
    @PostMapping("/{contractId}/questions")
    public ResponseEntity<QuestionResponse> ask(
            @PathVariable Long contractId,
            @Valid @RequestBody QuestionCreateRequest request,
            @AuthenticationPrincipal MemberDetails member) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contractQuestionService.ask(contractId, request, member));
    }
}
