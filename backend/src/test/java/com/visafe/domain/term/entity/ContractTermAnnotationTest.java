package com.visafe.domain.term.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.visafe.domain.contract.ContractFixtures;
import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.term.enums.TermField;
import com.visafe.domain.term.enums.ValueSource;
import com.visafe.domain.term.enums.WarningCode;
import org.junit.jupiter.api.Test;

class ContractTermAnnotationTest {

    private final ContractVersion version = ContractFixtures.uploaded().getCurrentVersion();

    @Test
    void editingReadValueIsEmployerEditAndRevertingIsAiAgain() {
        ContractTermAnnotation annotation =
                ContractTermAnnotation.read(version, TermField.MONTHLY_BASE_WAGE, "2000000", null);

        annotation.markEditedByEmployer(false);
        assertThat(annotation.getValueSource()).isEqualTo(ValueSource.EMPLOYER_EDIT);

        annotation.markEditedByEmployer(true);
        assertThat(annotation.getValueSource()).isEqualTo(ValueSource.AI);
    }

    @Test
    void fillingUnreadValueIsEmployerInput() {
        ContractTermAnnotation annotation = ContractTermAnnotation.unread(version, TermField.BREAK_MINUTES);

        annotation.markEditedByEmployer(false);
        assertThat(annotation.getValueSource()).isEqualTo(ValueSource.EMPLOYER_INPUT);

        annotation.markEditedByEmployer(true);
        assertThat(annotation.getValueSource()).isEqualTo(ValueSource.AI);
    }

    @Test
    void employerEditClearsNeedsCheck() {
        ContractTermAnnotation annotation = ContractTermAnnotation.read(version, TermField.WEEKLY_HOLIDAY, "일요일", null);
        annotation.changeWarning(WarningCode.NEEDS_CHECK);

        annotation.markEditedByEmployer(false);

        assertThat(annotation.getWarningCode()).isNull();
    }
}
