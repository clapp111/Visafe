package com.visafe.global.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.visafe.domain.member.entity.Member;
import com.visafe.domain.member.entity.MemberDetails;
import com.visafe.domain.member.service.MemberDetailsService;
import com.visafe.global.exception.CustomErrorCode;
import com.visafe.global.exception.RestApiException;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

class JwtTokenProviderTest {

    private static final String SECRET = "test-only-jwt-secret-at-least-32-bytes-long";

    private final MemberDetailsService memberDetailsService = mock(MemberDetailsService.class);
    private JwtTokenProvider provider;
    private MemberDetails member;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(
                new JwtProperties(SECRET, Duration.ofHours(3), Duration.ofDays(14)), memberDetailsService);

        Member worker = Member.worker("Worker@Example.com", "{bcrypt}hash", "Nguyen Van An");
        ReflectionTestUtils.setField(worker, "id", 7L);
        member = MemberDetails.from(worker);
        given(memberDetailsService.loadByMemberId(7L)).willReturn(Optional.of(member));
    }

    @Test
    void accessTokenAuthenticatesMemberWithRoleFromDatabase() {
        Authentication authentication = provider.getAuthentication(provider.createAccessToken(member));

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(((MemberDetails) authentication.getPrincipal()).getMemberId())
                .isEqualTo(7L);
        assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_WORKER");
    }

    @Test
    void refreshTokenCannotBeUsedAsAccessToken() {
        String refreshToken = provider.createRefreshToken(7L);

        assertThat(provider.getMemberIdFromRefreshToken(refreshToken)).isEqualTo(7L);
        assertErrorCode(() -> provider.getAuthentication(refreshToken), CustomErrorCode.JWT_INVALID);
    }

    @Test
    void accessTokenCannotBeUsedAsRefreshToken() {
        String accessToken = provider.createAccessToken(member);

        assertErrorCode(() -> provider.getMemberIdFromRefreshToken(accessToken), CustomErrorCode.JWT_INVALID);
    }

    @Test
    void refreshTokensAreUniqueEvenWhenIssuedAtTheSameTime() {
        assertThat(provider.createRefreshToken(7L)).isNotEqualTo(provider.createRefreshToken(7L));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtTokenProvider expiring = new JwtTokenProvider(
                new JwtProperties(SECRET, Duration.ofSeconds(-1), Duration.ofDays(14)), memberDetailsService);

        assertErrorCode(
                () -> expiring.getAuthentication(expiring.createAccessToken(member)), CustomErrorCode.JWT_EXPIRED);
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        JwtTokenProvider other = new JwtTokenProvider(
                new JwtProperties(
                        "another-secret-key-that-is-also-32-bytes!!", Duration.ofHours(3), Duration.ofDays(14)),
                memberDetailsService);

        assertErrorCode(
                () -> provider.getAuthentication(other.createAccessToken(member)), CustomErrorCode.JWT_MALFORMED);
    }

    @Test
    void garbageTokenIsRejected() {
        assertErrorCode(() -> provider.getAuthentication("not-a-jwt"), CustomErrorCode.JWT_MALFORMED);
    }

    @Test
    void tokenOfDeletedMemberIsRejected() {
        String accessToken = provider.createAccessToken(member);
        given(memberDetailsService.loadByMemberId(7L)).willReturn(Optional.empty());

        assertErrorCode(() -> provider.getAuthentication(accessToken), CustomErrorCode.JWT_INVALID);
    }

    @Test
    void refreshTokenIsStoredAsHashAndMatchesOnlyItself() {
        String token = provider.createRefreshToken(7L);
        RefreshToken stored = RefreshToken.issue(7L, token, java.time.Instant.now());

        assertThat(stored.getTokenHash()).matches("^0x[0-9a-f]{64}$").doesNotContain(token);
        assertThat(stored.matches(token)).isTrue();
        assertThat(stored.matches(provider.createRefreshToken(7L))).isFalse();
    }

    private static void assertErrorCode(Runnable call, CustomErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(RestApiException.class)
                .extracting(e -> ((RestApiException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
