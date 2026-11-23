package com.visafe.infra.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI(Claude) 설정 (application.yml의 visafe.llm).
 *
 * @param apiKey FactChat API 키. 비어 있으면 stub 구현을 쓴다
 * @param baseUrl Anthropic SDK용 Base URL
 */
@ConfigurationProperties("visafe.llm")
public record LlmProperties(String apiKey, String baseUrl) {}
