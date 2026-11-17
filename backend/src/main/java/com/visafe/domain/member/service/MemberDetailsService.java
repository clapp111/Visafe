package com.visafe.domain.member.service;

import com.visafe.domain.member.entity.Member;
import com.visafe.domain.member.entity.MemberDetails;
import com.visafe.domain.member.repository.MemberRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 로그인 이메일로 회원 인증 정보를 불러온다.
     *
     * <p>로그인 시 {@code AuthenticationManager}가 호출하며, 반환된 비밀번호 해시와 입력한 비밀번호를 BCrypt로 비교한다.
     * 이메일은 소문자로 정규화한 뒤 조회한다. 회원이 없을 때 던지는 {@code UsernameNotFoundException}은
     * Spring Security가 {@code BadCredentialsException}으로 바꿔 비밀번호 오류와 구분되지 않게 한다.</p>
     *
     * @param email 로그인 이메일
     * @return 회원 ID, 이메일, 비밀번호 해시, 역할을 담은 인증 정보
     */
    @Override
    public MemberDetails loadUserByUsername(String email) {
        return memberRepository
                .findByLoginEmail(Member.normalizeEmail(email))
                .map(MemberDetails::from)
                .orElseThrow(() -> new UsernameNotFoundException("회원을 찾을 수 없습니다."));
    }

    /**
     * 회원 ID로 회원 인증 정보를 불러온다.
     *
     * <p>액세스 토큰 인증과 토큰 재발급에서 매 요청 호출하므로, 역할 변경이나 탈퇴가 다음 요청부터 바로 반영된다.</p>
     *
     * @param memberId 토큰에 담긴 회원 ID
     * @return 회원 인증 정보, 회원이 없으면 빈 {@link Optional}
     */
    public Optional<MemberDetails> loadByMemberId(Long memberId) {
        return memberRepository.findById(memberId).map(MemberDetails::from);
    }
}
