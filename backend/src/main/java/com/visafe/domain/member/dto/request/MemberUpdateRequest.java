package com.visafe.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = "내 정보 수정 요청 DTO")
public record MemberUpdateRequest(
        @Schema(description = "화면 언어 (ko, vi)", example = "vi")
        @NotNull(message = "화면 언어를 입력해 주세요.")
        @Pattern(regexp = "^(ko|vi)$", message = "화면 언어는 ko 또는 vi만 쓸 수 있습니다.")
        String uiLanguage) {}
