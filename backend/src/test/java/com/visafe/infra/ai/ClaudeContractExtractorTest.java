package com.visafe.infra.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.anthropic.models.messages.OutputConfig;
import com.visafe.domain.contract.extraction.ExtractionResult;
import com.visafe.domain.term.enums.TermField;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClaudeContractExtractorTest {

    @Test
    void outputIsConvertedWithBlanksAsUnread() {
        ClaudeContractExtractor.Output output = new ClaudeContractExtractor.Output(
                List.of(
                        new ClaudeContractExtractor.Clause("", 0, "사업주와 근로자는 다음과 같이 근로계약을 체결한다."),
                        new ClaudeContractExtractor.Clause("제4조", 1, " 근로시간: 09시 00분부터 18시 00분까지 "),
                        new ClaudeContractExtractor.Clause("제5조", 1, "   ")),
                List.of(
                        new ClaudeContractExtractor.Term("workStartTime", "09:00", false, 1),
                        new ClaudeContractExtractor.Term("workStartTime", "10:00", false, 1),
                        new ClaudeContractExtractor.Term("breakMinutes", "", false, -1),
                        new ClaudeContractExtractor.Term("weeklyHoliday", " 일요일 ", true, -1),
                        new ClaudeContractExtractor.Term("overtimeRate", "50", false, 1)));

        ExtractionResult result = ClaudeContractExtractor.toResult(output);

        assertThat(result.clauses())
                .containsExactly(
                        new ExtractionResult.Clause(null, null, "사업주와 근로자는 다음과 같이 근로계약을 체결한다."),
                        new ExtractionResult.Clause("제4조", 1, "근로시간: 09시 00분부터 18시 00분까지"));
        assertThat(result.terms())
                .containsOnlyKeys(TermField.WORK_START_TIME, TermField.BREAK_MINUTES, TermField.WEEKLY_HOLIDAY);
        assertThat(result.terms().get(TermField.WORK_START_TIME))
                .isEqualTo(new ExtractionResult.Term("09:00", false, 1));
        assertThat(result.terms().get(TermField.BREAK_MINUTES)).isEqualTo(new ExtractionResult.Term(null, false, null));
        assertThat(result.terms().get(TermField.WEEKLY_HOLIDAY))
                .isEqualTo(new ExtractionResult.Term("일요일", true, null));
    }

    @Test
    void systemPromptListsAllTermKeys() {
        assertThat(ClaudeContractExtractor.SYSTEM_PROMPT)
                .contains(
                        Arrays.stream(TermField.values()).map(TermField::getKey).toList());
    }

    @Test
    void responseSchemaPassesLocalValidation() {
        // 응답 형식 레코드로 JSON 스키마를 만들 수 있는지 SDK가 요청을 만들 때 검사한다. (네트워크 호출 없음)
        assertThatCode(() -> ClaudeRequests.structured(
                                ClaudeContractExtractor.Output.class, OutputConfig.Effort.LOW, 1000)
                        .addUserMessage("확인")
                        .build())
                .doesNotThrowAnyException();
    }
}
