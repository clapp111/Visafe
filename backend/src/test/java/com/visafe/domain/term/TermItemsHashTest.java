package com.visafe.domain.term;

import static org.assertj.core.api.Assertions.assertThat;

import com.visafe.domain.contract.ContractFixtures;
import com.visafe.domain.term.entity.ContractLaborTerms;
import com.visafe.domain.term.enums.TermField;
import com.visafe.global.crypto.Keccak256;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class TermItemsHashTest {

    @Test
    void canonicalJsonHasAllColumnsInAlphabeticalOrderWithStringValues() {
        ContractLaborTerms terms = terms();

        assertThat(TermItemsHash.canonicalJson(terms))
                .isEqualTo("{\"accommodation\":null,\"break_minutes\":\"60\",\"employer_address\":null,"
                        + "\"employer_name\":\"한빛정밀\",\"end_date\":null,\"job_description\":null,"
                        + "\"monthly_base_wage\":null,\"pay_day\":null,\"pay_method\":null,"
                        + "\"representative_name\":null,\"start_date\":\"2027-01-02\",\"weekly_holiday\":null,"
                        + "\"work_days_per_week\":null,\"work_days_text\":null,\"work_end_time\":null,"
                        + "\"work_place\":null,\"work_start_time\":\"09:00\",\"worker_name\":null,"
                        + "\"worker_nationality\":null}");
    }

    @Test
    void hashIsKeccakOfUtf8CanonicalJson() {
        ContractLaborTerms terms = terms();

        assertThat(TermItemsHash.of(terms))
                .isEqualTo(Keccak256.hash(TermItemsHash.canonicalJson(terms).getBytes(StandardCharsets.UTF_8)))
                .matches("^0x[0-9a-f]{64}$");
    }

    @Test
    void anyValueChangeChangesHash() {
        ContractLaborTerms terms = terms();
        String before = TermItemsHash.of(terms);

        terms.changeValue(TermField.BREAK_MINUTES, 30);

        assertThat(TermItemsHash.of(terms)).isNotEqualTo(before);
    }

    private static ContractLaborTerms terms() {
        ContractLaborTerms terms =
                ContractLaborTerms.empty(ContractFixtures.uploaded().getCurrentVersion());
        terms.changeValue(TermField.EMPLOYER_NAME, "한빛정밀");
        terms.changeValue(TermField.START_DATE, LocalDate.of(2027, 1, 2));
        terms.changeValue(TermField.WORK_START_TIME, LocalTime.of(9, 0));
        terms.changeValue(TermField.BREAK_MINUTES, 60);
        return terms;
    }
}
