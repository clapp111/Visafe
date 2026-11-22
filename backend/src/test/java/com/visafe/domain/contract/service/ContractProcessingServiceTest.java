package com.visafe.domain.contract.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.visafe.domain.chain.entity.ChainRecord;
import com.visafe.domain.chain.enums.ChainRecordStatus;
import com.visafe.domain.chain.repository.ChainRecordRepository;
import com.visafe.domain.contract.ContractFixtures;
import com.visafe.domain.contract.dto.response.ProcessingResponse;
import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.entity.ContractEvent;
import com.visafe.domain.contract.enums.ContractEventType;
import com.visafe.domain.contract.enums.ContractStage;
import com.visafe.domain.contract.enums.ProcessingStatus;
import com.visafe.domain.contract.enums.ProcessingTask;
import com.visafe.domain.contract.repository.ContractEventRepository;
import com.visafe.domain.translation.entity.Translation;
import com.visafe.domain.translation.repository.TranslationRepository;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ContractProcessingServiceTest {

    private final ContractEventRepository eventRepository = mock(ContractEventRepository.class);
    private final TranslationRepository translationRepository = mock(TranslationRepository.class);
    private final ChainRecordRepository chainRecordRepository = mock(ChainRecordRepository.class);
    private final ContractProcessingService service =
            new ContractProcessingService(eventRepository, translationRepository, chainRecordRepository);

    @Test
    void extractionRunningAfterStart() {
        Contract contract = ContractFixtures.uploaded();
        givenEvents(contract, ContractEventType.FILE_UPLOADED, ContractEventType.EXTRACTION_STARTED);

        assertThat(service.resolve(contract)).isEqualTo(ProcessingResponse.running(ProcessingTask.EXTRACTION, 1));
    }

    @Test
    void extractionFailedKeepsErrorFromPayload() {
        Contract contract = ContractFixtures.uploaded();
        when(eventRepository.findVersionEvents(any(), any()))
                .thenReturn(java.util.List.of(
                        event(contract, ContractEventType.FILE_UPLOADED, Map.of()),
                        event(contract, ContractEventType.EXTRACTION_STARTED, Map.of()),
                        event(contract, ContractEventType.EXTRACTION_FAILED, Map.of("error", "OCR 시간 초과"))));

        ProcessingResponse processing = service.resolve(contract);

        assertThat(processing.status()).isEqualTo(ProcessingStatus.FAILED);
        assertThat(processing.attempt()).isEqualTo(1);
        assertThat(processing.lastError()).isEqualTo("OCR 시간 초과");
    }

    @Test
    void extractionRetryCountsAttemptsAndShowsRunning() {
        Contract contract = ContractFixtures.uploaded();
        givenEvents(
                contract,
                ContractEventType.FILE_UPLOADED,
                ContractEventType.EXTRACTION_STARTED,
                ContractEventType.EXTRACTION_FAILED,
                ContractEventType.EXTRACTION_STARTED);

        assertThat(service.resolve(contract)).isEqualTo(ProcessingResponse.running(ProcessingTask.EXTRACTION, 2));
    }

    @Test
    void reuploadResetsExtractionAttempts() {
        Contract contract = ContractFixtures.uploaded();
        givenEvents(
                contract,
                ContractEventType.FILE_UPLOADED,
                ContractEventType.EXTRACTION_STARTED,
                ContractEventType.EXTRACTION_FAILED,
                ContractEventType.FILE_UPLOADED,
                ContractEventType.EXTRACTION_STARTED);

        assertThat(service.resolve(contract)).isEqualTo(ProcessingResponse.running(ProcessingTask.EXTRACTION, 1));
    }

    @Test
    void translationBeforeFirstAttemptIsRunning() {
        Contract contract = ContractFixtures.inStage(ContractStage.TRANSLATING);
        when(translationRepository.findFirstByContractVersionIdOrderByTranslationNoDesc(any()))
                .thenReturn(Optional.empty());

        assertThat(service.resolve(contract)).isEqualTo(ProcessingResponse.running(ProcessingTask.TRANSLATION, 1));
    }

    @Test
    void translationFailureWithRetriesLeftIsStillRunning() {
        Contract contract = ContractFixtures.inStage(ContractStage.TRANSLATING);
        Translation translation = Translation.create(contract.getCurrentVersion(), "vi", 1);
        translation.start();
        translation.fail("timeout");
        givenTranslation(translation);

        assertThat(service.resolve(contract)).isEqualTo(ProcessingResponse.running(ProcessingTask.TRANSLATION, 1));
    }

    @Test
    void translationFailedThreeTimesIsFailed() {
        Contract contract = ContractFixtures.inStage(ContractStage.TRANSLATING);
        Translation translation = Translation.create(contract.getCurrentVersion(), "vi", 1);
        for (int i = 0; i < Translation.MAX_ATTEMPTS; i++) {
            translation.start();
            translation.fail("timeout " + (i + 1));
        }
        givenTranslation(translation);

        assertThat(service.resolve(contract))
                .isEqualTo(ProcessingResponse.failed(ProcessingTask.TRANSLATION, 3, "timeout 3"));
    }

    @Test
    void chainRecordUsesRetryCountAsAttempt() {
        Contract contract = ContractFixtures.inStage(ContractStage.RECORDING);
        ChainRecord record = mock(ChainRecord.class);
        when(record.getRetryCount()).thenReturn(1);
        when(record.getStatus()).thenReturn(ChainRecordStatus.FAILED);
        when(chainRecordRepository.findFirstByContractVersionIdAndRecordTypeOrderByIdDesc(any(), any()))
                .thenReturn(Optional.of(record));

        assertThat(service.resolve(contract))
                .isEqualTo(ProcessingResponse.failed(ProcessingTask.CHAIN_RECORD, 2, null));
    }

    @Test
    void chainRecordNotCreatedYetIsRunning() {
        Contract contract = ContractFixtures.inStage(ContractStage.RECORDING);
        when(chainRecordRepository.findFirstByContractVersionIdAndRecordTypeOrderByIdDesc(any(), any()))
                .thenReturn(Optional.empty());

        assertThat(service.resolve(contract)).isEqualTo(ProcessingResponse.running(ProcessingTask.CHAIN_RECORD, 1));
    }

    @Test
    void stagesWithoutBackgroundWorkHaveNoProcessing() {
        assertThat(service.resolve(ContractFixtures.inStage(ContractStage.REVIEWING)))
                .isNull();
        assertThat(service.resolve(ContractFixtures.inStage(ContractStage.AWAITING_WORKER)))
                .isNull();
    }

    private void givenEvents(Contract contract, ContractEventType... types) {
        when(eventRepository.findVersionEvents(any(), any()))
                .thenReturn(Arrays.stream(types)
                        .map(type -> event(contract, type, Map.of()))
                        .toList());
    }

    private void givenTranslation(Translation translation) {
        when(translationRepository.findFirstByContractVersionIdOrderByTranslationNoDesc(any()))
                .thenReturn(Optional.of(translation));
    }

    private static ContractEvent event(Contract contract, ContractEventType type, Map<String, Object> payload) {
        return ContractEvent.bySystem(contract, contract.getCurrentVersion(), type, payload);
    }
}
