package com.visafe.domain.member.controller;

import com.visafe.domain.member.dto.request.MemberUpdateRequest;
import com.visafe.domain.member.dto.response.MemberResponse;
import com.visafe.domain.member.entity.MemberDetails;
import com.visafe.domain.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member", description = "회원")
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "내 정보 조회", description = "로그인한 회원의 정보를 조회한다.")
    @GetMapping("/me")
    public MemberResponse getMe(@AuthenticationPrincipal MemberDetails member) {
        return memberService.getMember(member.getMemberId());
    }

    @Operation(summary = "화면 언어 변경", description = "화면 언어(ko, vi)를 저장한다. 응답은 내 정보 조회와 같다.")
    @PatchMapping("/me")
    public MemberResponse updateMe(
            @AuthenticationPrincipal MemberDetails member, @Valid @RequestBody MemberUpdateRequest request) {
        return memberService.changeUiLanguage(member.getMemberId(), request.uiLanguage());
    }
}
