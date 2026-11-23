package com.visafe.global.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** API 키가 있을 때만 Claude API 클라이언트를 만든다. */
@Configuration
@ConditionalOnExpression(LlmCondition.ENABLED)
public class LlmConfig {

    @Bean
    public AnthropicClient anthropicClient(LlmProperties properties) {
        return AnthropicOkHttpClient.builder()
                .baseUrl(properties.baseUrl())
                .apiKey(properties.apiKey())
                .build();
    }
}
