package com.visafe.infra.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.anthropic.models.messages.OutputConfig;
import com.visafe.domain.review.answerer.QuestionAnswer;
import com.visafe.domain.review.answerer.QuestionSource;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClaudeContractQuestionAnswererTest {

    @Test
    void userMessageNumbersClausesAndAddsQuestion() {
        QuestionSource source = new QuestionSource(
                "vi",
                "야근하면 돈을 더 받나요?",
                List.of(
                        new QuestionSource.Clause(null, "사업주와 근로자는 근로계약을 체결한다."),
                        new QuestionSource.Clause("제9조", "연장·야간·휴일근로 시에는 통상임금의 50%를 가산하여 지급한다.")));

        assertThat(ClaudeContractQuestionAnswerer.userMessage(source))
                .contains("질문 언어: vi")
                .contains("0. 사업주와 근로자는 근로계약을 체결한다.")
                .contains("1. 제9조 연장·야간·휴일근로")
                .endsWith("[질문]\n야근하면 돈을 더 받나요?");
    }

    @Test
    void blankQuotedTextFallsBackToClauseText() {
        ClaudeContractQuestionAnswerer.Output output = new ClaudeContractQuestionAnswerer.Output(
                "Có. ...",
                true,
                List.of(
                        new ClaudeContractQuestionAnswerer.Citation(1, "통상임금의 50%를 가산하여 지급한다."),
                        new ClaudeContractQuestionAnswerer.Citation(0, " ")));

        QuestionAnswer answer = ClaudeContractQuestionAnswerer.toAnswer(output);

        assertThat(answer.answer()).isEqualTo("Có. ...");
        assertThat(answer.referredToCenter()).isTrue();
        assertThat(answer.citations())
                .containsExactly(
                        new QuestionAnswer.Citation(1, "통상임금의 50%를 가산하여 지급한다."), new QuestionAnswer.Citation(0, null));
    }

    @Test
    void responseSchemaPassesLocalValidation() {
        // 응답 형식 레코드로 JSON 스키마를 만들 수 있는지 SDK가 요청을 만들 때 검사한다. (네트워크 호출 없음)
        assertThatCode(() -> ClaudeRequests.structured(
                                ClaudeContractQuestionAnswerer.Output.class, OutputConfig.Effort.LOW, 1000)
                        .addUserMessage("확인")
                        .build())
                .doesNotThrowAnyException();
    }
}
