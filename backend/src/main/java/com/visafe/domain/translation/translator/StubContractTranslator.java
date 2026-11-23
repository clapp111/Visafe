package com.visafe.domain.translation.translator;

import com.visafe.domain.term.enums.TermField;
import com.visafe.global.config.LlmCondition;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * AI API 키(LLM_API_KEY)가 없을 때 쓰는 번역기. 번역하지 않고 원문 앞에 언어 표시(예: [vi], 한국어 번역은 [ko])를 붙여 돌려준다.
 * 고용주가 고친 값도 그대로 보이므로 근로자 확인 화면과 해시 흐름을 확인할 수 있다.
 */
@Component
@ConditionalOnExpression(LlmCondition.DISABLED)
public class StubContractTranslator implements ContractTranslator {

    @Override
    public TranslationResult translate(TranslationSource source) {
        String tag = "[" + source.language() + "] ";
        List<String> clauses =
                source.clauses().stream().map(clause -> tag + clause.textKo()).toList();
        Map<TermField, TranslationResult.Item> items = new EnumMap<>(TermField.class);
        source.terms()
                .forEach((field, value) -> items.put(
                        field,
                        new TranslationResult.Item(
                                tag + value,
                                tag + field.getLabel() + ": " + value,
                                field.getLabel() + "은(는) " + value + "입니다.")));
        return new TranslationResult(clauses, items);
    }

    @Override
    public String translateToKorean(String text, String language) {
        return "[ko] " + text;
    }
}
