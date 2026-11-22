package com.visafe.domain.lookup;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.Normalizer;
import org.junit.jupiter.api.Test;

class WorkerNameMaskTest {

    @Test
    void keepsLastWordAndFirstLetterOfOthers() {
        assertThat(WorkerNameMask.mask("Nguyễn Văn An")).isEqualTo("N***** V** An");
        assertThat(WorkerNameMask.mask("  Lê   Thị Hoa ")).isEqualTo("L* T** Hoa");
    }

    @Test
    void countsDecomposedVietnameseLettersAsOne() {
        String decomposed = Normalizer.normalize("Nguyễn Văn An", Normalizer.Form.NFD);

        assertThat(WorkerNameMask.mask(decomposed)).isEqualTo("N***** V** An");
    }

    @Test
    void singleWordNameIsMaskedAfterFirstLetter() {
        assertThat(WorkerNameMask.mask("Hoa")).isEqualTo("H**");
    }
}
