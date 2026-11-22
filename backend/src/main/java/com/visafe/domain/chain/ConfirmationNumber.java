package com.visafe.domain.chain;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Optional;

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

    /**
     * 사용자가 입력한 확인번호를 정리한다. 공백을 지우고 대문자로 바꾼 뒤 하이픈을 {@code VS-XXXX-XXXX} 자리에 맞춘다.
     * ({@code vs7k3f9qxm}, {@code VS 7K3F 9QXM} → {@code VS-7K3F-9QXM})
     *
     * @return 형식과 검사 문자가 맞으면 정리한 번호, 아니면 빈 값
     */
    public static Optional<String> normalize(String input) {
        if (input == null) {
            return Optional.empty();
        }
        String compact = input.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
        if (compact.length() != 10 || !compact.startsWith("VS")) {
            return Optional.empty();
        }
        String confirmationNo = "VS-" + compact.substring(2, 6) + "-" + compact.substring(6);
        return isValid(confirmationNo) ? Optional.of(confirmationNo) : Optional.empty();
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
