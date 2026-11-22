package com.visafe.domain.chain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class ConfirmationNumberTest {

    @RepeatedTest(20)
    void generatedNumberMatchesDbFormatAndPassesCheck() {
        String confirmationNo = ConfirmationNumber.generate();

        assertThat(confirmationNo).matches("^VS-[2-9A-HJ-NP-Z]{4}-[2-9A-HJ-NP-Z]{4}$");
        assertThat(ConfirmationNumber.isValid(confirmationNo)).isTrue();
    }

    @Test
    void anySingleCharacterTypoIsDetected() {
        String confirmationNo = ConfirmationNumber.generate();

        for (int position : new int[] {3, 4, 5, 6, 8, 9, 10, 11}) {
            for (char c : ConfirmationNumber.ALPHABET.toCharArray()) {
                if (c == confirmationNo.charAt(position)) {
                    continue;
                }
                String typo = confirmationNo.substring(0, position) + c + confirmationNo.substring(position + 1);
                assertThat(ConfirmationNumber.isValid(typo)).as(typo).isFalse();
            }
        }
    }

    @Test
    void wrongFormatIsInvalid() {
        assertThat(ConfirmationNumber.isValid(null)).isFalse();
        assertThat(ConfirmationNumber.isValid("VS-7K3F-9QX")).isFalse();
        assertThat(ConfirmationNumber.isValid("VS-7K3F-9QX0")).isFalse();
        assertThat(ConfirmationNumber.isValid("vs-7k3f-9qxm")).isFalse();
    }
}
