package com.visafe.domain.translation;

import com.visafe.domain.translation.entity.TranslationClause;
import com.visafe.domain.translation.entity.TranslationItem;
import com.visafe.global.crypto.Keccak256;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import tools.jackson.databind.json.JsonMapper;

/**
 * 번역 해시(translations.translation_hash)를 계산한다. 블록체인 기록(hash_rule_version 1)이 같은 값을 쓴다.
 * <p>
 * 규칙: 근로자에게 보이는 내용만 담은 공백 없는 JSON 객체를 UTF-8로 바꿔 keccak256을 구한다.
 * <pre>
 * {"clauses":[{"seq":1,"text":"…"},…],"items":{"break_minutes":{"easyExplanation":"…","translatedValue":"…"},…},"language":"vi"}
 * </pre>
 * 조항은 seq 순서, 항목은 DB 컬럼 이름 알파벳순이며 객체 키도 알파벳순이다.
 * 고용주 검토용 한국어 설명(easy_explanation_ko)은 넣지 않는다. 한 번 기록된 해시를 다시 계산할 수 있어야 하므로 규칙을 바꾸지 않는다.
 */
public final class TranslationHash {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private TranslationHash() {}

    /** 번역본 내용의 해시를 0x + 소문자 16진수 64자리로 돌려준다. */
    public static String of(String language, List<TranslationClause> clauses, List<TranslationItem> items) {
        return Keccak256.hash(canonicalJson(language, clauses, items).getBytes(StandardCharsets.UTF_8));
    }

    /** 해시 입력이 되는 JSON 문자열을 만든다. */
    static String canonicalJson(String language, List<TranslationClause> clauses, List<TranslationItem> items) {
        List<Map<String, Object>> clauseList = new ArrayList<>();
        clauses.stream()
                .sorted(Comparator.comparingInt(
                        clause -> clause.getContractClause().getSeq()))
                .forEach(clause -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("seq", clause.getContractClause().getSeq());
                    entry.put("text", clause.getTranslatedText());
                    clauseList.add(entry);
                });

        Map<String, Map<String, String>> itemMap = new TreeMap<>();
        for (TranslationItem item : items) {
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("easyExplanation", item.getEasyExplanation());
            entry.put("translatedValue", item.getTranslatedValue());
            itemMap.put(item.getFieldName().getColumnName(), entry);
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("clauses", clauseList);
        root.put("items", itemMap);
        root.put("language", language);
        return JSON.writeValueAsString(root);
    }
}
