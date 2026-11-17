package com.visafe.global.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CORS 허용 출처 (application.yml의 cors.allowed-origins).
 * 쿠키(리프레시 토큰)를 주고받으므로 "*" 대신 프론트 주소만 허용한다.
 */
@ConfigurationProperties("cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }
}
