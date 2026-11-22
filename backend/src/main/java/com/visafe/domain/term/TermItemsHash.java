package com.visafe.domain.term;

import com.visafe.domain.term.entity.ContractLaborTerms;
import com.visafe.domain.term.enums.TermField;
import com.visafe.global.crypto.Keccak256;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import tools.jackson.databind.json.JsonMapper;

/**
 * 항목 해시(contract_versions.items_hash)를 계산한다. 블록체인 기록(hash_rule_version 1)이 같은 값을 쓴다.
 * <p>
 * 규칙: 19개 항목을 DB 컬럼 이름 알파벳순으로 담은 공백 없는 JSON 객체를 UTF-8로 바꿔 keccak256을 구한다.
 * 값은 모두 문자열이며(날짜 2027-01-02, 시각 09:00, 숫자 60), 빈 항목은 {@code null}이다.
 * 한 번 기록된 해시를 다시 계산할 수 있어야 하므로 규칙을 바꾸지 않는다.
 */
public final class TermItemsHash {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private TermItemsHash() {}

    /** 항목 값의 해시를 0x + 소문자 16진수 64자리로 돌려준다. */
    public static String of(ContractLaborTerms terms) {
        return Keccak256.hash(canonicalJson(terms).getBytes(StandardCharsets.UTF_8));
    }

    /** 해시 입력이 되는 JSON 문자열을 만든다. */
    static String canonicalJson(ContractLaborTerms terms) {
        Map<String, String> items = new TreeMap<>();
        for (TermField field : TermField.values()) {
            Object value = terms.getValue(field);
            items.put(field.getColumnName(), value == null ? null : value.toString());
        }
        return JSON.writeValueAsString(items);
    }
}
