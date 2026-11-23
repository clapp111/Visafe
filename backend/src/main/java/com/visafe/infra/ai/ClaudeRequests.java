package com.visafe.infra.ai;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.StructuredOutputConfig;
import com.anthropic.models.messages.StructuredTextBlock;
import com.anthropic.models.messages.TextBlock;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Claude API 요청의 공통 설정과 응답 확인.
 * <p>
 * 모델은 Claude Sonnet 5.5이고 thinking은 기본값(adaptive)이다. 안전 분류기가 요청을 거절하면 서버가 대체 모델로 다시 처리하도록
 * 서버 측 fallback(beta)을 켠다. 그래도 거절되거나 출력이 max_tokens에 걸려 잘리면 예외를 던져 호출한 쪽의 실패 처리로 넘긴다.
 */
public final class ClaudeRequests {

    public static final String MODEL = "claude-sonnet-5-5";
    private static final String FALLBACK_BETA = "server-side-fallback-2026-07-01";

    private ClaudeRequests() {}

    /** 응답을 주어진 타입으로 받는 요청을 만든다. */
    public static <T> StructuredMessageCreateParams.Builder<T> structured(
            Class<T> outputType, OutputConfig.Effort effort, long maxTokens) {
        return base(maxTokens)
                .outputConfig(StructuredOutputConfig.<T>builder()
                        .effort(effort)
                        .format(outputType)
                        .build());
    }

    /** 응답을 글자로 받는 요청을 만든다. */
    public static MessageCreateParams.Builder text(OutputConfig.Effort effort, long maxTokens) {
        return base(maxTokens)
                .outputConfig(OutputConfig.builder().effort(effort).build());
    }

    /** 구조화 응답에서 결과 객체를 꺼낸다. 끝까지 답하지 못했으면 예외를 던진다. */
    public static <T> T result(StructuredMessage<T> message) {
        requireCompleted(message.stopReason());
        return message.content().stream()
                .flatMap(block -> block.text().stream())
                .map(StructuredTextBlock::text)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Claude 응답에 결과가 없습니다."));
    }

    /** 글자 응답을 이어 붙여 꺼낸다. 끝까지 답하지 못했으면 예외를 던진다. */
    public static String text(Message message) {
        requireCompleted(message.stopReason());
        String text = message.content().stream()
                .flatMap(block -> block.text().stream())
                .map(TextBlock::text)
                .collect(Collectors.joining())
                .strip();
        if (text.isEmpty()) {
            throw new IllegalStateException("Claude 응답이 비어 있습니다.");
        }
        return text;
    }

    /** 거절(refusal)이나 출력 한도(max_tokens)로 끝났으면 예외를 던진다. */
    static void requireCompleted(Optional<StopReason> stopReason) {
        if (stopReason.isPresent()
                && (stopReason.get().equals(StopReason.REFUSAL)
                        || stopReason.get().equals(StopReason.MAX_TOKENS))) {
            throw new IllegalStateException("Claude가 끝까지 답하지 못했습니다. stop_reason=" + stopReason.get());
        }
    }

    private static MessageCreateParams.Builder base(long maxTokens) {
        return MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(maxTokens)
                .putAdditionalHeader("anthropic-beta", FALLBACK_BETA)
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"));
    }
}
