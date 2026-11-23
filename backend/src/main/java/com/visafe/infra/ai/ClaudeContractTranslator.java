package com.visafe.infra.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.OutputConfig;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.visafe.domain.term.enums.TermField;
import com.visafe.domain.translation.translator.ContractTranslationWorker;
import com.visafe.domain.translation.translator.ContractTranslator;
import com.visafe.domain.translation.translator.TranslationResult;
import com.visafe.domain.translation.translator.TranslationSource;
import com.visafe.global.config.LlmCondition;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Claude가 조항과 항목을 근로자의 언어로 번역하고 항목마다 쉬운 설명을 붙인다. 신고 사유의 한국어 번역도 맡는다.
 * <p>
 * 계약 번역은 effort medium, 신고 사유 번역은 low다. 응답이 원문 조항·항목과 맞는지는 번역을 저장하는 쪽({@link ContractTranslationWorker})이 검사한다.
 */
@Component
@ConditionalOnExpression(LlmCondition.ENABLED)
@RequiredArgsConstructor
public class ClaudeContractTranslator implements ContractTranslator {

    private static final long MAX_TOKENS = 16000;
    private static final long REASON_MAX_TOKENS = 2000;

    static final String SYSTEM_PROMPT = """
            당신은 한국에서 일하는 외국인 근로자가 자신의 근로계약서를 이해하도록 돕는 번역가입니다.
            근로자의 언어는 ISO 639-1 코드로 주어집니다.

            [조항 clauses]
            - 입력 조항마다 하나씩, 같은 개수와 같은 순서로 근로자의 언어로 번역합니다.
            - 뜻을 바꾸거나 빼거나 덧붙이지 말고 원문 그대로 옮깁니다.

            [항목 items]
            - 입력 항목마다 하나씩, key는 입력의 키를 그대로 씁니다.
            - translatedValue: 값을 근로자의 언어로 옮깁니다. 금액에는 원 단위를, 날짜와 시각은 읽기 쉬운 형식을 씁니다.
            - easyExplanation: 이 항목이 근로자에게 무엇을 뜻하는지 근로자의 언어로 1~2문장으로 쉽게 설명합니다.
            - easyExplanationKo: 같은 설명을 고용주가 확인할 수 있도록 한국어로 씁니다.
            - 계약서에 적힌 내용만 설명하고, 위법 여부 판단이나 법률 조언은 하지 않습니다.
            """;

    static final String REASON_SYSTEM_PROMPT = """
            외국인 근로자가 자신의 근로계약서에 대해 쓴 글을 고용주가 읽을 수 있도록 자연스러운 한국어로 번역합니다.
            번역문만 출력하고 설명이나 따옴표는 붙이지 않습니다.
            """;

    private final AnthropicClient anthropicClient;

    @Override
    public TranslationResult translate(TranslationSource source) {
        Output output = ClaudeRequests.result(anthropicClient
                .messages()
                .create(ClaudeRequests.structured(Output.class, OutputConfig.Effort.MEDIUM, MAX_TOKENS)
                        .system(SYSTEM_PROMPT)
                        .addUserMessage(userMessage(source))
                        .build()));
        return toResult(output);
    }

    @Override
    public String translateToKorean(String text, String language) {
        return ClaudeRequests.text(anthropicClient
                .messages()
                .create(ClaudeRequests.text(OutputConfig.Effort.LOW, REASON_MAX_TOKENS)
                        .system(REASON_SYSTEM_PROMPT)
                        .addUserMessage("언어: " + language + "\n\n" + text)
                        .build()));
    }

    /** 번역할 조항과 항목을 위치 번호를 붙여 한 메시지로 만든다. */
    static String userMessage(TranslationSource source) {
        StringBuilder message =
                new StringBuilder("근로자 언어: ").append(source.language()).append("\n\n[조항]\n");
        for (int i = 0; i < source.clauses().size(); i++) {
            TranslationSource.Clause clause = source.clauses().get(i);
            message.append(i).append(". ");
            if (clause.clauseNo() != null) {
                message.append(clause.clauseNo()).append(' ');
            }
            message.append(clause.textKo()).append('\n');
        }
        message.append("\n[항목] (키 | 이름 | 값)\n");
        for (TermField field : TermField.values()) {
            String value = source.terms().get(field);
            if (value != null) {
                message.append("- ")
                        .append(field.getKey())
                        .append(" | ")
                        .append(field.getLabel())
                        .append(" | ")
                        .append(value)
                        .append('\n');
            }
        }
        return message.toString();
    }

    /** Claude 응답을 번역 결과로 바꾼다. 없는 항목 키는 버리고, 같은 키가 여러 번 오면 처음 것을 쓴다. */
    static TranslationResult toResult(Output output) {
        Map<TermField, TranslationResult.Item> items = new EnumMap<>(TermField.class);
        for (Item item : output.items()) {
            TermField.fromKey(item.key())
                    .ifPresent(field -> items.putIfAbsent(
                            field,
                            new TranslationResult.Item(
                                    item.translatedValue(), item.easyExplanation(), item.easyExplanationKo())));
        }
        return new TranslationResult(output.clauses(), items);
    }

    /**
     * Claude 응답 형식.
     *
     * @param clauses 조항 번역 (입력과 같은 개수·순서)
     * @param items 항목 번역 (입력 항목마다 하나)
     */
    record Output(
            @JsonPropertyDescription("입력 조항과 같은 개수·순서의 번역문") List<String> clauses, List<Item> items) {}

    record Item(
            @JsonPropertyDescription("입력 항목의 키") String key,
            @JsonPropertyDescription("근로자 언어로 옮긴 값") String translatedValue,

            @JsonPropertyDescription("근로자 언어로 쓴 1~2문장의 쉬운 설명")
            String easyExplanation,

            @JsonPropertyDescription("같은 설명의 한국어") String easyExplanationKo) {}
}
