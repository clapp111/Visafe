package com.visafe.global.jwt;

import com.visafe.global.exception.ErrorCode;
import com.visafe.global.exception.ErrorResponseWriter;
import com.visafe.global.exception.RestApiException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * {@link JwtAuthenticationFilter}에서 토큰 검증에 실패하면(RestApiException) 에러 응답으로 바꾼다.
 * 필터에서 난 예외는 GlobalExceptionHandler까지 가지 않기 때문에 여기서 처리한다.
 */
@Slf4j
public class JwtExceptionFilter extends OncePerRequestFilter {

    private final ErrorResponseWriter errorResponseWriter;

    public JwtExceptionFilter(ErrorResponseWriter errorResponseWriter) {
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (RestApiException e) {
            ErrorCode errorCode = e.getErrorCode();
            log.warn("[인증 오류] 토큰 검증 실패 - code={}, 사유: {}", errorCode.getCode(), errorCode.getMessage());
            errorResponseWriter.write(response, errorCode);
        }
    }
}
