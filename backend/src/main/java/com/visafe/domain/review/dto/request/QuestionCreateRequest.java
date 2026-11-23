package com.visafe.domain.review.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "질문하기 요청 DTO")
public record QuestionCreateRequest(
        @Schema(description = "질문 (1~500자)", example = "야근하면 돈을 더 받나요?")
        @NotBlank(message = "질문을 입력해 주세요.")
        @Size(max = 500, message = "질문은 500자를 넘을 수 없습니다.")
        String question) {}
