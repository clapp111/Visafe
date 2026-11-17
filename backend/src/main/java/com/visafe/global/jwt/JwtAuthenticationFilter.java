package com.visafe.global.jwt;

import com.visafe.domain.member.entity.MemberDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authorization: Bearer 액세스 토큰을 검증해 SecurityContext에 회원을 넣는다.
 * 토큰이 없으면 그냥 통과시키고(공개 API용), 토큰이 잘못되었으면 {@link JwtExceptionFilter}가 401로 응답한다.
 *
 * <p>@Component로 등록하면 서블릿 필터로도 한 번 더 등록되므로 SecurityConfig에서 직접 생성한다.
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_API_PREFIX = "/api/v1/auth/";
    private static final String MDC_MEMBER_ID = "memberId";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /** 로그인·재발급·로그아웃은 만료된 액세스 토큰이 헤더에 남아 있어도 동작해야 하므로 검사하지 않는다. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith(AUTH_API_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            Authentication authentication = jwtTokenProvider.getAuthentication(token);
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            MemberDetails member = (MemberDetails) authentication.getPrincipal();
            MDC.put(MDC_MEMBER_ID, String.valueOf(member.getMemberId()));
            log.debug("[인증 성공] memberId: {}", member.getMemberId());
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_MEMBER_ID);
        }
    }

    private static String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
