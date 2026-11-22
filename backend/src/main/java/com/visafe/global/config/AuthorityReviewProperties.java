package com.visafe.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 감독 기관 검토 단계 설정 (application.yml의 visafe.authority-review).
 * 계약을 만들 때 이 값을 contracts.requires_authority_review로 복사한다.
 *
 * @param enabled 새 계약이 기관 검토를 거치는지 여부
 */
@ConfigurationProperties("visafe.authority-review")
public record AuthorityReviewProperties(boolean enabled) {}
