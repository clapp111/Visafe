package com.visafe.domain.member.service;

import com.visafe.domain.member.dto.response.MemberResponse;
import com.visafe.domain.member.repository.MemberRepository;
import com.visafe.global.exception.CustomErrorCode;
import com.visafe.global.exception.RestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 회원 정보를 조회한다.
     *
     * @param memberId 조회할 회원 ID
     * @return 회원 ID, 로그인 이메일, 이름, 역할, 화면 언어를 담은 회원 정보
     * @throws RestApiException 회원을 찾을 수 없는 경우.
     */
    public MemberResponse getMember(Long memberId) {
        return memberRepository
                .findById(memberId)
                .map(MemberResponse::from)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.MEMBER_NOT_FOUND));
    }
}
