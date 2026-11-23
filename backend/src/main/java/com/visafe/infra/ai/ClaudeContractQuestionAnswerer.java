package com.visafe.infra.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.OutputConfig;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.visafe.domain.review.answerer.ContractQuestionAnswerer;
import com.visafe.domain.review.answerer.QuestionAnswer;
import com.visafe.domain.review.answerer.QuestionSource;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Claude가 이 계약서 조항만 근거로 근로자의 질문에 답하고 근거 조항을 인용한다.
 * <p>
 * 질문하기는 최대 20초만 기다리므로 effort는 low이고, 요청 시간 제한을 20초로 두고 SDK 재시도는 하지 않는다.
 * 인용 조항이 범위를 벗어나거나 겹치는지는 답을 저장하는 쪽이 검사한다.
 */
@Component
@ConditionalOnExpression(LlmCondition.ENABLED)
public class ClaudeContractQuestionAnswerer implements ContractQuestionAnswerer {

    private static final long MAX_TOKENS = 4000;
    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    static final String SYSTEM_PROMPT = """
            당신은 한국에서 일하는 외국인 근로자가 자신의 근로계약서에 대해 묻는 질문에 답합니다.
            - 주어진 계약서 조항만 근거로 답하고, 계약서에 없는 내용은 없다고 말합니다. 추측하지 마세요.
            - 답은 질문 언어(ISO 639-1 코드로 주어짐)로, 짧고 쉬운 문장으로 씁니다.
            - 위법 여부, 받을 수 있는 권리나 보상처럼 법률 판단이 필요한 질문이면 referredToCenter를 true로 두고,
              답에 고용노동부 외국인력상담센터(1350)에 문의하라는 안내를 넣습니다.
            - 이유와 상관없이 답에 상담센터(1350) 안내를 넣었다면 referredToCenter는 반드시 true입니다.
            - citations에는 답의 근거가 된 조항을 넣습니다. clauseIndex는 조항 앞의 위치 번호(0부터)이고,
              quotedText는 그 조항에서 근거가 된 문장을 원문 그대로 옮깁니다. 근거가 없으면 빈 목록입니다.
            """;

    private final AnthropicClient anthropicClient;

    public ClaudeContractQuestionAnswerer(AnthropicClient anthropicClient) {
        this.anthropicClient =
                anthropicClient.withOptions(options -> options.timeout(TIMEOUT).maxRetries(0));
    }

    @Override
    public QuestionAnswer answer(QuestionSource source) {
        Output output = ClaudeRequests.result(anthropicClient
                .messages()
                .create(ClaudeRequests.structured(Output.class, OutputConfig.Effort.LOW, MAX_TOKENS)
                        .system(SYSTEM_PROMPT)
                        .addUserMessage(userMessage(source))
                        .build()));
        return toAnswer(output);
    }

    /** 조항에 위치 번호를 붙이고 질문을 덧붙여 한 메시지로 만든다. */
    static String userMessage(QuestionSource source) {
        StringBuilder message =
                new StringBuilder("질문 언어: ").append(source.language()).append("\n\n[계약서 조항]\n");
        for (int i = 0; i < source.clauses().size(); i++) {
            QuestionSource.Clause clause = source.clauses().get(i);
            message.append(i).append(". ");
            if (clause.clauseNo() != null) {
                message.append(clause.clauseNo()).append(' ');
            }
            message.append(clause.textKo()).append('\n');
        }
        return message.append("\n[질문]\n").append(source.question()).toString();
    }

    /** Claude 응답을 답으로 바꾼다. 비어 있는 인용 문장은 조항 원문을 쓰도록 비운다. */
    static QuestionAnswer toAnswer(Output output) {
        return new QuestionAnswer(
                output.answer(),
                output.referredToCenter(),
                output.citations().stream()
                        .map(citation -> new QuestionAnswer.Citation(
                                citation.clauseIndex(),
                                citation.quotedText() == null
                                                || citation.quotedText().isBlank()
                                        ? null
                                        : citation.quotedText()))
                        .toList());
    }

    /**
     * Claude 응답 형식.
     *
     * @param answer 질문 언어로 쓴 답
     * @param referredToCenter 상담센터(1350) 안내 여부
     * @param citations 근거 조항
     */
    record Output(
            @JsonPropertyDescription("질문 언어로 쓴 짧고 쉬운 답") String answer,

            @JsonPropertyDescription("법률 판단이 필요해 1350 상담센터를 안내하는지")
            boolean referredToCenter,

            List<Citation> citations) {}

    record Citation(
            @JsonPropertyDescription("근거 조항의 위치 번호(0부터)") int clauseIndex,

            @JsonPropertyDescription("그 조항에서 근거가 된 문장 (원문 그대로)")
            String quotedText) {}
}
