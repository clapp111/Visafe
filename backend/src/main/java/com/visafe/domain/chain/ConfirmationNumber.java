package com.visafe.domain.chain;

import java.security.SecureRandom;

/**
 * 공개 확인번호 (contracts.confirmation_no). 형식은 {@code VS-XXXX-XXXX}이며 DB CHECK(ck_contracts__confirmation_no)와 같다.
 * <p>
 * 헷갈리는 글자(0, O, 1, I)를 뺀 32자 중에서 앞 7자리는 무작위로 뽑고, 마지막 글자는 앞 7자리로 계산한 검사 문자다.
 * 검사 문자는 자리마다 홀수 가중치(1, 3, 5, …)를 곱한 합을 32로 나눈 나머지라서 한 글자만 잘못 입력한 경우를 모두 찾는다.
 */
public final class ConfirmationNumber {

    static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int BODY_LENGTH = 7;
    private static final SecureRandom RANDOM = new SecureRandom();

    private ConfirmationNumber() {}

    /** 새 확인번호를 만든다. (다른 계약과 겹치는지는 호출하는 쪽이 확인) */
    public static String generate() {
        StringBuilder body = new StringBuilder(BODY_LENGTH);
        for (int i = 0; i < BODY_LENGTH; i++) {
            body.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        String code = body.toString() + checkChar(body);
        return "VS-" + code.substring(0, 4) + "-" + code.substring(4);
    }

    /** 형식과 검사 문자가 맞는지 확인한다. */
    public static boolean isValid(String confirmationNo) {
        if (confirmationNo == null || !confirmationNo.matches("^VS-[2-9A-HJ-NP-Z]{4}-[2-9A-HJ-NP-Z]{4}$")) {
            return false;
        }
        String code = confirmationNo.substring(3, 7) + confirmationNo.substring(8);
        return code.charAt(BODY_LENGTH) == checkChar(code.substring(0, BODY_LENGTH));
    }

    private static char checkChar(CharSequence body) {
        int sum = 0;
        for (int i = 0; i < BODY_LENGTH; i++) {
            sum += (2 * i + 1) * ALPHABET.indexOf(body.charAt(i));
        }
        return ALPHABET.charAt(sum % ALPHABET.length());
    }
}
