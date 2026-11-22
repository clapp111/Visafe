package com.visafe.global.config;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 연도별 최저임금 시급 (application.yml의 visafe.minimum-wage). 계약 시작일이 속한 연도의 금액과 비교한다.
 * 새 연도 금액이 고시되면 한 줄 추가한다.
 *
 * @param hourly 연도별 시급(원)
 */
@ConfigurationProperties("visafe.minimum-wage")
public record MinimumWageProperties(Map<Integer, Integer> hourly) {

    public MinimumWageProperties {
        hourly = hourly == null ? Map.of() : Map.copyOf(hourly);
    }
}
