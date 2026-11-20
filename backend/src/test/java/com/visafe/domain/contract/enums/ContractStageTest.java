package com.visafe.domain.contract.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ContractStageTest {

    @Test
    void reviewingCanGoBackToUploadedForReupload() {
        assertThat(ContractStage.REVIEWING.canTransitionTo(ContractStage.UPLOADED))
                .isTrue();
        assertThat(ContractStage.REVIEWING.canTransitionTo(ContractStage.TRANSLATING))
                .isTrue();
    }

    @Test
    void awaitingWorkerCannotGoBackToTranslating() {
        // 번역 신고는 MVP에서 제외되어 근로자 확인 대기에서 다시 번역으로 돌아가지 않는다.
        assertThat(ContractStage.AWAITING_WORKER.canTransitionTo(ContractStage.TRANSLATING))
                .isFalse();
    }

    @Test
    void uploadedCanOnlyGoToReviewing() {
        for (ContractStage next : ContractStage.values()) {
            assertThat(ContractStage.UPLOADED.canTransitionTo(next)).isEqualTo(next == ContractStage.REVIEWING);
        }
    }
}
