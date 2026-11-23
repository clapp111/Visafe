package com.visafe.infra.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.anthropic.models.messages.OutputConfig;
import com.visafe.domain.term.enums.TermField;
import com.visafe.domain.translation.translator.TranslationResult;
import com.visafe.domain.translation.translator.TranslationSource;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ClaudeContractTranslatorTest {

    @Test
    void userMessageNumbersClausesAndListsOnlyFilledTerms() {
        TranslationSource source = new TranslationSource(
                "vi",
                List.of(
                        new TranslationSource.Clause(null, "사업주와 근로자는 근로계약을 체결한다."),
                        new TranslationSource.Clause("제6조", "임금: 월 기본급 2,000,000원")),
                Map.of(TermField.MONTHLY_BASE_WAGE, "2000000", TermField.PAY_DAY, "10"));

        String message = ClaudeContractTranslator.userMessage(source);

        assertThat(message)
                .contains("근로자 언어: vi")
                .contains("0. 사업주와 근로자는 근로계약을 체결한다.")
                .contains("1. 제6조 임금: 월 기본급 2,000,000원")
                .contains("- monthlyBaseWage | 월 기본급(원) | 2000000")
                .contains("- payDay | 임금 지급일 | 10")
                .doesNotContain("breakMinutes");
        // 항목은 TermField 순서로 나온다.
        assertThat(message.indexOf("monthlyBaseWage")).isLessThan(message.indexOf("payDay"));
    }

    @Test
    void outputIsConvertedDroppingUnknownKeys() {
        ClaudeContractTranslator.Output output = new ClaudeContractTranslator.Output(
                List.of("Điều 1", "Điều 6"),
                List.of(
                        new ClaudeContractTranslator.Item("payDay", "ngày 10", "Lương trả ngày 10", "임금은 10일에 받습니다."),
                        new ClaudeContractTranslator.Item("payDay", "ngày 11", "x", "x"),
                        new ClaudeContractTranslator.Item("bonus", "x", "x", "x")));

        TranslationResult result = ClaudeContractTranslator.toResult(output);

        assertThat(result.clauses()).containsExactly("Điều 1", "Điều 6");
        assertThat(result.items())
                .containsOnlyKeys(TermField.PAY_DAY)
                .containsEntry(
                        TermField.PAY_DAY,
                        new TranslationResult.Item("ngày 10", "Lương trả ngày 10", "임금은 10일에 받습니다."));
    }

    @Test
    void responseSchemaPassesLocalValidation() {
        // 응답 형식 레코드로 JSON 스키마를 만들 수 있는지 SDK가 요청을 만들 때 검사한다. (네트워크 호출 없음)
        assertThatCode(() -> ClaudeRequests.structured(
                                ClaudeContractTranslator.Output.class, OutputConfig.Effort.LOW, 1000)
                        .addUserMessage("확인")
                        .build())
                .doesNotThrowAnyException();
    }
}
