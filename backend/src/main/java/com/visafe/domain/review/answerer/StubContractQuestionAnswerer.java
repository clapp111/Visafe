package com.visafe.domain.review.answerer;

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 실제 LLM 연동 전까지 쓰는 답변기. 질문과 단어(두 글자 이상)가 가장 많이 겹치는 조항 하나를 근거로 인용하고,
 * 그 조항을 확인하라는 고정 문장을 언어 표시(예: [vi])와 함께 돌려준다. 상담센터 안내는 하지 않는다.
 */
@Component
public class StubContractQuestionAnswerer implements ContractQuestionAnswerer {

    @Override
    public QuestionAnswer answer(QuestionSource source) {
        String tag = "[" + source.language() + "] ";
        List<String> words = Arrays.stream(source.question().split("[^\\p{L}\\p{N}]+"))
                .filter(word -> word.length() >= 2)
                .toList();
        int best = -1;
        long bestScore = 0;
        for (int i = 0; i < source.clauses().size(); i++) {
            String text = source.clauses().get(i).textKo();
            long score = words.stream().filter(text::contains).count();
            if (score > bestScore) {
                best = i;
                bestScore = score;
            }
        }
        if (best < 0) {
            return new QuestionAnswer(tag + "계약서에서 관련 조항을 찾지 못했습니다.", false, List.of());
        }
        QuestionSource.Clause clause = source.clauses().get(best);
        String name = clause.clauseNo() == null ? "해당 조항" : clause.clauseNo();
        return new QuestionAnswer(
                tag + "계약서 " + name + "를 확인해 주세요.", false, List.of(new QuestionAnswer.Citation(best, clause.textKo())));
    }
}
