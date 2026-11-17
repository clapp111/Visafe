package com.visafe.global.jwt;

import com.visafe.global.exception.CustomErrorCode;
import com.visafe.global.exception.ErrorResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * 로그인은 했지만 권한이 없는 API(예: /api/v1/admin/**)를 호출한 경우 403 응답을 반환한다.
 *
 * <p>설정하지 않으면 Spring Security 기본값으로 body 없는 403이 반환된다.
 */
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ErrorResponseWriter errorResponseWriter;

    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        errorResponseWriter.write(response, CustomErrorCode.AUTH_FORBIDDEN);
    }
}
