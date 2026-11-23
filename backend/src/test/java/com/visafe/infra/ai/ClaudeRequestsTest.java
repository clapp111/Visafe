package com.visafe.infra.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ClaudeRequestsTest {

    record Answer(String text) {}

    @Test
    void structuredRequestUsesSonnetEffortAndServerSideFallback() {
        StructuredMessageCreateParams<Answer> params = ClaudeRequests.structured(
                        Answer.class, OutputConfig.Effort.HIGH, 16000)
                .addUserMessage("안녕하세요")
                .build();

        assertThat(params.model().asString()).isEqualTo("claude-sonnet-5-5");
        assertThat(params.maxTokens()).isEqualTo(16000);
        assertThat(params.outputConfig().flatMap(OutputConfig::effort)).contains(OutputConfig.Effort.HIGH);
        assertThat(params.outputConfig().flatMap(OutputConfig::format)).isPresent();
        assertThat(params._additionalHeaders().values("anthropic-beta"))
                .containsExactly("server-side-fallback-2026-07-01");
        assertThat(params._additionalBodyProperties()).containsEntry("fallbacks", JsonValue.from("default"));
    }

    @Test
    void textRequestUsesEffortWithoutFormat() {
        MessageCreateParams params = ClaudeRequests.text(OutputConfig.Effort.LOW, 2000)
                .addUserMessage("안녕하세요")
                .build();

        assertThat(params.outputConfig().flatMap(OutputConfig::effort)).contains(OutputConfig.Effort.LOW);
        assertThat(params.outputConfig().flatMap(OutputConfig::format)).isEmpty();
        assertThat(params._additionalHeaders().values("anthropic-beta"))
                .containsExactly("server-side-fallback-2026-07-01");
    }

    @Test
    void refusalAndMaxTokensAreFailures() {
        for (StopReason stopReason : List.of(StopReason.REFUSAL, StopReason.MAX_TOKENS)) {
            assertThatThrownBy(() -> ClaudeRequests.requireCompleted(Optional.of(stopReason)))
                    .isInstanceOf(IllegalStateException.class);
        }
        assertThatCode(() -> ClaudeRequests.requireCompleted(Optional.of(StopReason.END_TURN)))
                .doesNotThrowAnyException();
        assertThatCode(() -> ClaudeRequests.requireCompleted(Optional.empty())).doesNotThrowAnyException();
    }
}
