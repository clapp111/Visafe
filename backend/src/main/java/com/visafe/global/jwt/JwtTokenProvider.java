package com.visafe.global.jwt;

import com.visafe.domain.member.entity.MemberDetails;
import com.visafe.domain.member.service.MemberDetailsService;
import com.visafe.global.exception.CustomErrorCode;
import com.visafe.global.exception.RestApiException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * JWT 발급·검증.
 * <ul>
 *   <li>subject = 회원 ID (이메일이 바뀌어도 토큰은 유지)</li>
 *   <li>type 클레임으로 액세스·리프레시 토큰을 구분해, 리프레시 토큰을 액세스 토큰으로 쓰지 못하게 한다.</li>
 *   <li>권한은 토큰이 아니라 매 요청 DB의 회원 역할로 판단한다. (역할 변경·탈퇴가 바로 반영)</li>
 * </ul>
 */
@Component
public class JwtTokenProvider {

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_ROLE = "role";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final JwtProperties jwtProperties;
    private final MemberDetailsService memberDetailsService;

    public JwtTokenProvider(JwtProperties jwtProperties, MemberDetailsService memberDetailsService) {
        // 32바이트 미만이면 WeakKeyException으로 서버 시작이 실패한다.
        this.key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        this.jwtProperties = jwtProperties;
        this.memberDetailsService = memberDetailsService;
    }

    public String createAccessToken(MemberDetails member) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(member.getMemberId()))
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .claim(CLAIM_ROLE, member.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(jwtProperties.accessTokenValidity())))
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(Long memberId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString()) // 같은 시각에 발급해도 토큰이 겹치지 않게
                .subject(String.valueOf(memberId))
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(jwtProperties.refreshTokenValidity())))
                .signWith(key)
                .compact();
    }

    /** 액세스 토큰을 검증하고 회원 인증 정보를 만든다. 유효하지 않으면 {@link RestApiException}. */
    public Authentication getAuthentication(String accessToken) {
        Long memberId = parseMemberId(parseClaims(accessToken, TYPE_ACCESS));
        MemberDetails member = memberDetailsService
                .loadByMemberId(memberId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.JWT_INVALID));
        return UsernamePasswordAuthenticationToken.authenticated(member, null, member.getAuthorities());
    }

    /** 리프레시 토큰을 검증하고 회원 ID를 꺼낸다. 유효하지 않으면 {@link RestApiException}. */
    public Long getMemberIdFromRefreshToken(String refreshToken) {
        return parseMemberId(parseClaims(refreshToken, TYPE_REFRESH));
    }

    private Claims parseClaims(String token, String expectedType) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new RestApiException(CustomErrorCode.JWT_EXPIRED);
        } catch (MalformedJwtException | io.jsonwebtoken.security.SecurityException e) {
            throw new RestApiException(CustomErrorCode.JWT_MALFORMED);
        } catch (UnsupportedJwtException e) {
            throw new RestApiException(CustomErrorCode.JWT_UNSUPPORTED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new RestApiException(CustomErrorCode.JWT_INVALID);
        }
        if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new RestApiException(CustomErrorCode.JWT_INVALID);
        }
        return claims;
    }

    private static Long parseMemberId(Claims claims) {
        try {
            return Long.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            throw new RestApiException(CustomErrorCode.JWT_INVALID);
        }
    }
}
