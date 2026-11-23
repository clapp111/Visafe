package com.visafe.domain.member.controller;

import com.visafe.domain.member.dto.request.MemberUpdateRequest;
import com.visafe.domain.member.dto.response.MemberResponse;
import com.visafe.domain.member.entity.MemberDetails;
import com.visafe.domain.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController implements MemberControllerDocument {

    private final MemberService memberService;

    @Override
    @GetMapping("/me")
    public ResponseEntity<MemberResponse> getMe(@AuthenticationPrincipal MemberDetails member) {
        return ResponseEntity.ok(memberService.getMember(member.getMemberId()));
    }

    @Override
    @PatchMapping("/me")
    public ResponseEntity<MemberResponse> updateMe(
            @AuthenticationPrincipal MemberDetails member, @Valid @RequestBody MemberUpdateRequest request) {
        return ResponseEntity.ok(memberService.changeUiLanguage(member.getMemberId(), request.uiLanguage()));
    }
}
