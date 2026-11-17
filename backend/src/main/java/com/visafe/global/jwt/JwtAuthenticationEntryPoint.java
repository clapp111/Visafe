package com.visafe.global.jwt;

import com.visafe.global.exception.CustomErrorCode;
import com.visafe.global.exception.ErrorResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 인증 정보 없이 인증이 필요한 API를 호출한 경우 401 응답을 반환한다.
 *
 * <p>설정하지 않으면 Spring Security 기본값으로 body 없는 403이 반환된다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ErrorResponseWriter errorResponseWriter;

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        errorResponseWriter.write(response, CustomErrorCode.AUTH_UNAUTHORIZED);
    }
}
