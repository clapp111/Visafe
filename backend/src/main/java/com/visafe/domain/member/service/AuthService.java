package com.visafe.domain.member.service;

import com.visafe.domain.member.entity.Member;
import com.visafe.domain.member.entity.MemberDetails;
import com.visafe.global.exception.CustomErrorCode;
import com.visafe.global.exception.RestApiException;
import com.visafe.global.jwt.JwtProperties;
import com.visafe.global.jwt.JwtTokenProvider;
import com.visafe.global.jwt.RefreshToken;
import com.visafe.global.jwt.RefreshTokenRepository;
import com.visafe.global.jwt.TokenResponse;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final MemberDetailsService memberDetailsService;
    private final RefreshTokenRepository refreshTokenRepository;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 이메일과 비밀번호로 로그인하고 액세스·리프레시 토큰을 발급한다.
     *
     * <p>이메일은 대소문자를 구분하지 않는다. 이메일이 없거나 비밀번호가 틀리면 {@code BadCredentialsException}이 발생하며,
     * 가입 여부를 알 수 없도록 두 경우 모두 같은 {@link CustomErrorCode#AUTH_LOGIN_FAILED} 응답으로 처리된다.
     * 회원당 리프레시 토큰은 1개라서 다른 기기에서 로그인하면 이전 기기의 리프레시 토큰은 무효가 된다.</p>
     *
     * @param email 로그인 이메일
     * @param password 평문 비밀번호
     * @return 액세스 토큰과 리프레시 토큰을 담은 발급 결과
     */
    @Transactional
    public TokenResponse login(String email, String password) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(Member.normalizeEmail(email), password));
        MemberDetails member = (MemberDetails) authentication.getPrincipal();
        log.info("[로그인] 성공 - memberId={}", member.getMemberId());
        return issueTokens(member);
    }

    /**
     * 리프레시 토큰으로 액세스·리프레시 토큰을 다시 발급한다.
     *
     * <p>발급할 때마다 리프레시 토큰을 새 값으로 바꾸므로(rotation) 한 번 쓴 리프레시 토큰은 다시 쓸 수 없다.
     * 저장된 토큰과 다르면 이미 재발급되었거나 탈취된 토큰으로 보고 거부한다.</p>
     *
     * @param refreshToken 쿠키로 전달된 리프레시 토큰
     * @return 새로 발급한 액세스 토큰과 리프레시 토큰
     * @throws RestApiException 리프레시 토큰이 만료·위조되었거나 형식이 잘못된 경우,
     *                          저장된 토큰이나 회원을 찾을 수 없는 경우,
     *                          또는 저장된 토큰과 일치하지 않는 경우.
     */
    @Transactional
    public TokenResponse reissue(String refreshToken) {
        Long memberId = jwtTokenProvider.getMemberIdFromRefreshToken(refreshToken);
        RefreshToken stored = refreshTokenRepository
                .findByMemberId(memberId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.JWT_REFRESH_NOT_FOUND));
        if (!stored.matches(refreshToken)) {
            // 이미 바뀐(재발급·다른 기기 로그인) 토큰 또는 탈취된 토큰
            throw new RestApiException(CustomErrorCode.JWT_REFRESH_NOT_MATCH);
        }
        MemberDetails member = memberDetailsService
                .loadByMemberId(memberId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.JWT_REFRESH_NOT_FOUND));
        return issueTokens(member);
    }

    /**
     * 저장된 리프레시 토큰을 삭제한다.
     *
     * <p>토큰이 없거나 이미 만료·무효인 경우에도 예외 없이 성공으로 처리한다.
     * 저장된 토큰과 일치할 때만 삭제하므로 이전에 쓰던 토큰으로 다른 기기의 로그인을 끊을 수 없다.</p>
     *
     * @param refreshToken 쿠키로 전달된 리프레시 토큰, 없으면 {@code null}
     */
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        try {
            Long memberId = jwtTokenProvider.getMemberIdFromRefreshToken(refreshToken);
            refreshTokenRepository
                    .findByMemberId(memberId)
                    .filter(stored -> stored.matches(refreshToken))
                    .ifPresent(refreshTokenRepository::delete);
        } catch (RestApiException e) {
            log.debug("[로그아웃] 이미 무효한 리프레시 토큰 - code={}", e.getErrorCode().getCode());
        }
    }

    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * 액세스·리프레시 토큰을 만들고 리프레시 토큰을 저장한다.
     *
     * <p>회원의 리프레시 토큰이 이미 있으면 새 토큰으로 교체하고, 없으면 새로 저장한다.</p>
     *
     * @param member 토큰을 발급할 회원
     * @return 액세스 토큰과 리프레시 토큰을 담은 발급 결과
     */
    private TokenResponse issueTokens(MemberDetails member) {
        String accessToken = jwtTokenProvider.createAccessToken(member);
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getMemberId());
        Instant refreshExpiresAt = Instant.now().plus(jwtProperties.refreshTokenValidity());

        refreshTokenRepository
                .findByMemberId(member.getMemberId())
                .ifPresentOrElse(
                        stored -> stored.rotate(refreshToken, refreshExpiresAt),
                        () -> refreshTokenRepository.save(
                                RefreshToken.issue(member.getMemberId(), refreshToken, refreshExpiresAt)));

        return TokenResponse.of(accessToken, jwtProperties.accessTokenValidity(), refreshToken);
    }
}
