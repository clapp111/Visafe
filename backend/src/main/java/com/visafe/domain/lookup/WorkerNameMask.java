package com.visafe.domain.lookup;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * 공개 조회에 보여 줄 근로자 이름을 가린다. 마지막 단어만 남기고 나머지 단어는 첫 글자 뒤를 {@code *}로 바꾼다.
 * ({@code Nguyễn Văn An} → {@code N***** V** An}) 한 단어 이름은 그대로 드러나지 않도록 그 단어의 첫 글자 뒤를 가린다.
 * 성조가 있는 글자도 한 글자로 세도록 NFC로 정규화한 뒤 코드 포인트 단위로 센다.
 */
public final class WorkerNameMask {

    private WorkerNameMask() {}

    public static String mask(String name) {
        String[] words = Normalizer.normalize(name.strip(), Normalizer.Form.NFC).split("\\s+", -1);
        if (words.length == 1) {
            return maskWord(words[0]);
        }
        List<String> masked = new ArrayList<>();
        for (int i = 0; i < words.length - 1; i++) {
            masked.add(maskWord(words[i]));
        }
        masked.add(words[words.length - 1]);
        return String.join(" ", masked);
    }

    private static String maskWord(String word) {
        int[] codePoints = word.codePoints().toArray();
        return new String(codePoints, 0, 1) + "*".repeat(codePoints.length - 1);
    }
}
