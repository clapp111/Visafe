package com.visafe.domain.member.dto.response;

import com.visafe.domain.member.entity.Member;
import com.visafe.domain.member.enums.MemberRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원 정보 응답 DTO")
public record MemberResponse(
        @Schema(description = "회원 ID", example = "1") Long id,

        @Schema(description = "로그인 이메일", example = "employer@visafe.kr")
        String email,

        @Schema(description = "이름", example = "홍길동") String name,
        @Schema(description = "역할", example = "EMPLOYER") MemberRole role,
        @Schema(description = "화면 언어", example = "ko") String uiLanguage) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(), member.getLoginEmail(), member.getName(), member.getRole(), member.getUiLanguage());
    }
}
