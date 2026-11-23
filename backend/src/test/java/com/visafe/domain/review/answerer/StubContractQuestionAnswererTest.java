package com.visafe.domain.review.answerer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class StubContractQuestionAnswererTest {

    private static final List<QuestionSource.Clause> CLAUSES = List.of(
            new QuestionSource.Clause(null, "사업주와 근로자는 다음과 같이 근로계약을 체결한다."),
            new QuestionSource.Clause("제8조", "숙식 제공: 숙소 제공(기숙사), 식사 중식 제공"),
            new QuestionSource.Clause("제9조", "연장·야간·휴일근로 시에는 통상임금의 50%를 가산하여 지급한다."));

    private final StubContractQuestionAnswerer answerer = new StubContractQuestionAnswerer();

    @Test
    void citesClauseSharingMostWords() {
        QuestionAnswer answer = answerer.answer(new QuestionSource("vi", "연장 야간 근로하면 돈을 더 받나요?", CLAUSES));

        assertThat(answer.answer()).isEqualTo("[vi] 계약서 제9조를 확인해 주세요.");
        assertThat(answer.referredToCenter()).isFalse();
        assertThat(answer.citations())
                .containsExactly(new QuestionAnswer.Citation(2, CLAUSES.get(2).textKo()));
    }

    @Test
    void answersWithoutCitationWhenNoClauseMatches() {
        QuestionAnswer answer = answerer.answer(new QuestionSource("vi", "휴가는 며칠인가요?", CLAUSES));

        assertThat(answer.answer()).startsWith("[vi] ");
        assertThat(answer.citations()).isEmpty();
    }
}
