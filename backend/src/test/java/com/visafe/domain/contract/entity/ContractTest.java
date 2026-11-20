package com.visafe.domain.contract.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.visafe.domain.company.entity.Company;
import com.visafe.domain.contract.enums.ContractStage;
import com.visafe.domain.contract.enums.ContractStatus;
import com.visafe.domain.member.entity.Member;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ContractTest {

    private static final String CONFIRMATION_NO = "VS-7K3F-9QXM";

    @Test
    void reuploadMovesReviewingBackToUploaded() {
        Contract contract = newContract(false);
        contract.changeStage(ContractStage.REVIEWING);

        contract.changeStage(ContractStage.UPLOADED);

        assertThat(contract.getStage()).isEqualTo(ContractStage.UPLOADED);
    }

    @Test
    void startRecordingKeepsConfirmationNoBeforeConfirmed() {
        Contract contract = awaitingWorker(false);

        contract.startRecording(CONFIRMATION_NO);

        assertThat(contract.getStage()).isEqualTo(ContractStage.RECORDING);
        assertThat(contract.getConfirmationNo()).isEqualTo(CONFIRMATION_NO);
        assertThat(contract.getConfirmedAt()).isNull();
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.IN_PROGRESS);
    }

    @Test
    void confirmUsesNumberReservedAtRecording() {
        Contract contract = awaitingWorker(false);
        contract.startRecording(CONFIRMATION_NO);
        Instant confirmedAt = Instant.parse("2026-11-13T05:06:00Z");

        contract.confirm(confirmedAt);

        assertThat(contract.getStage()).isEqualTo(ContractStage.DONE);
        assertThat(contract.getConfirmationNo()).isEqualTo(CONFIRMATION_NO);
        assertThat(contract.getConfirmedAt()).isEqualTo(confirmedAt);
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.CONFIRMED);
    }

    @Test
    void recordingCannotBeEnteredWithoutConfirmationNo() {
        Contract contract = awaitingWorker(false);

        assertThatThrownBy(() -> contract.changeStage(ContractStage.RECORDING))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> contract.startRecording(null)).isInstanceOf(NullPointerException.class);
        assertThat(contract.getStage()).isEqualTo(ContractStage.AWAITING_WORKER);
    }

    @Test
    void authorityReviewContractMustBeApprovedBeforeRecording() {
        Contract contract = awaitingWorker(true);

        assertThatThrownBy(() -> contract.startRecording(CONFIRMATION_NO)).isInstanceOf(IllegalStateException.class);

        contract.changeStage(ContractStage.AUTHORITY_REVIEW);
        contract.startRecording(CONFIRMATION_NO);

        assertThat(contract.getStage()).isEqualTo(ContractStage.RECORDING);
        assertThat(contract.getConfirmationNo()).isEqualTo(CONFIRMATION_NO);
    }

    @Test
    void contractWithoutAuthorityReviewCannotEnterAuthorityReview() {
        Contract contract = awaitingWorker(false);

        assertThatThrownBy(() -> contract.changeStage(ContractStage.AUTHORITY_REVIEW))
                .isInstanceOf(IllegalStateException.class);
    }

    private static Contract awaitingWorker(boolean requiresAuthorityReview) {
        Contract contract = newContract(requiresAuthorityReview);
        contract.changeStage(ContractStage.REVIEWING);
        contract.changeStage(ContractStage.TRANSLATING);
        contract.changeStage(ContractStage.AWAITING_WORKER);
        return contract;
    }

    private static Contract newContract(boolean requiresAuthorityReview) {
        Company company = Company.create("한빛정밀", "김민수", "000-00-00001", "경기도 안산시 단원구 데모로 1");
        Member employer = Member.employer("employer@demo.visafe.test", "hash", "김민수", company);
        Member worker = Member.worker("worker1@demo.visafe.test", "hash", "Nguyễn Văn An");
        return Contract.create("C-2026-00001", company, employer, worker, "vi", null, requiresAuthorityReview);
    }
}
