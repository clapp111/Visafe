package com.visafe.domain.member.service;

import com.visafe.domain.member.dto.response.MemberResponse;
import com.visafe.domain.member.entity.Member;
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
        return MemberResponse.from(findMember(memberId));
    }

    /**
     * 회원의 화면 언어를 바꾼다.
     *
     * <p>화면 상단의 언어 버튼이 호출한다. 이후 새로 만드는 알림도 이 언어로 쓴다.</p>
     *
     * @param memberId 화면 언어를 바꿀 회원 ID
     * @param uiLanguage 바꿀 화면 언어 ({@code ko} 또는 {@code vi}, 요청 DTO에서 검증)
     * @return 바뀐 화면 언어를 담은 회원 정보
     * @throws RestApiException 회원을 찾을 수 없는 경우
     */
    @Transactional
    public MemberResponse changeUiLanguage(Long memberId, String uiLanguage) {
        Member member = findMember(memberId);
        member.changeUiLanguage(uiLanguage);
        return MemberResponse.from(member);
    }

    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * 회원을 찾는다.
     *
     * @param memberId 찾을 회원 ID
     * @return 회원
     * @throws RestApiException 회원을 찾을 수 없는 경우
     */
    private Member findMember(Long memberId) {
        return memberRepository
                .findById(memberId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.MEMBER_NOT_FOUND));
    }
}
