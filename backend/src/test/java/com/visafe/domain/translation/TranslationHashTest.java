package com.visafe.domain.translation;

import static org.assertj.core.api.Assertions.assertThat;

import com.visafe.domain.contract.ContractFixtures;
import com.visafe.domain.contract.entity.ContractClause;
import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.term.enums.TermField;
import com.visafe.domain.translation.entity.Translation;
import com.visafe.domain.translation.entity.TranslationClause;
import com.visafe.domain.translation.entity.TranslationItem;
import java.util.List;
import org.junit.jupiter.api.Test;

class TranslationHashTest {

    private final ContractVersion version = ContractFixtures.uploaded().getCurrentVersion();
    private final Translation translation = Translation.create(version, "vi", 1);

    @Test
    void canonicalJsonSortsClausesBySeqAndItemsByColumnName() {
        List<TranslationClause> clauses = List.of(clause(2, "Điều 2"), clause(1, "Điều 1"));
        List<TranslationItem> items = List.of(
                item(TermField.PAY_DAY, "ngày 10", "Lương trả ngày 10", "임금은 10일에 받습니다."),
                item(TermField.BREAK_MINUTES, "60 phút", "Nghỉ 60 phút", "휴게시간은 60분입니다."));

        assertThat(TranslationHash.canonicalJson("vi", clauses, items))
                .isEqualTo("{\"clauses\":[{\"seq\":1,\"text\":\"Điều 1\"},{\"seq\":2,\"text\":\"Điều 2\"}],"
                        + "\"items\":{\"break_minutes\":{\"easyExplanation\":\"Nghỉ 60 phút\",\"translatedValue\":\"60 phút\"},"
                        + "\"pay_day\":{\"easyExplanation\":\"Lương trả ngày 10\",\"translatedValue\":\"ngày 10\"}},"
                        + "\"language\":\"vi\"}");
    }

    @Test
    void koreanExplanationForEmployerIsNotHashed() {
        List<TranslationClause> clauses = List.of(clause(1, "Điều 1"));

        String hash = TranslationHash.of(
                "vi", clauses, List.of(item(TermField.PAY_DAY, "ngày 10", "Lương trả ngày 10", "설명 A")));
        String sameExceptKorean = TranslationHash.of(
                "vi", clauses, List.of(item(TermField.PAY_DAY, "ngày 10", "Lương trả ngày 10", "설명 B")));
        String explanationChanged = TranslationHash.of(
                "vi", clauses, List.of(item(TermField.PAY_DAY, "ngày 10", "Trả lương ngày 10", "설명 A")));

        assertThat(hash).matches("^0x[0-9a-f]{64}$").isEqualTo(sameExceptKorean).isNotEqualTo(explanationChanged);
    }

    private TranslationClause clause(int seq, String text) {
        ContractClause source = ContractClause.create(version.getContractFile(), seq, "제" + seq + "조", 1, "원문");
        return TranslationClause.of(translation, source, text);
    }

    private TranslationItem item(TermField field, String value, String explanation, String explanationKo) {
        return TranslationItem.builder()
                .translation(translation)
                .fieldName(field)
                .translatedValue(value)
                .easyExplanation(explanation)
                .easyExplanationKo(explanationKo)
                .build();
    }
}
