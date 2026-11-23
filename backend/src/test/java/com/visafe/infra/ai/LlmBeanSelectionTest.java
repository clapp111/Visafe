package com.visafe.infra.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.anthropic.client.AnthropicClient;
import com.visafe.domain.contract.extraction.ContractExtractor;
import com.visafe.domain.contract.extraction.StubContractExtractor;
import com.visafe.domain.review.answerer.ContractQuestionAnswerer;
import com.visafe.domain.review.answerer.StubContractQuestionAnswerer;
import com.visafe.domain.translation.translator.ContractTranslator;
import com.visafe.domain.translation.translator.StubContractTranslator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** API 키가 있으면 Claude 구현, 없으면 stub 구현이 빈으로 뜨는지 확인한다. (네트워크 호출 없음) */
class LlmBeanSelectionTest {

    @Configuration
    @EnableConfigurationProperties(LlmProperties.class)
    @Import({
        LlmConfig.class,
        ClaudeContractExtractor.class,
        ClaudeContractTranslator.class,
        ClaudeContractQuestionAnswerer.class,
        StubContractExtractor.class,
        StubContractTranslator.class,
        StubContractQuestionAnswerer.class
    })
    static class Components {}

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Components.class)
            .withPropertyValues("visafe.llm.base-url=http://localhost");

    @Test
    void apiKeySelectsClaudeImplementations() {
        runner.withPropertyValues("visafe.llm.api-key=test-key").run(context -> {
            assertThat(context).hasSingleBean(AnthropicClient.class);
            assertThat(context.getBean(ContractExtractor.class)).isInstanceOf(ClaudeContractExtractor.class);
            assertThat(context.getBean(ContractTranslator.class)).isInstanceOf(ClaudeContractTranslator.class);
            assertThat(context.getBean(ContractQuestionAnswerer.class))
                    .isInstanceOf(ClaudeContractQuestionAnswerer.class);
        });
    }

    @Test
    void blankApiKeySelectsStubs() {
        for (String apiKey : new String[] {"", "  "}) {
            runner.withPropertyValues("visafe.llm.api-key=" + apiKey).run(context -> {
                assertThat(context).doesNotHaveBean(AnthropicClient.class);
                assertThat(context.getBean(ContractExtractor.class)).isInstanceOf(StubContractExtractor.class);
                assertThat(context.getBean(ContractTranslator.class)).isInstanceOf(StubContractTranslator.class);
                assertThat(context.getBean(ContractQuestionAnswerer.class))
                        .isInstanceOf(StubContractQuestionAnswerer.class);
            });
        }
        runner.run(context ->
                assertThat(context.getBean(ContractExtractor.class)).isInstanceOf(StubContractExtractor.class));
    }
}
