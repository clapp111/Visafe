package com.visafe.domain.contract.service;

import com.visafe.domain.chain.entity.ChainRecord;
import com.visafe.domain.chain.enums.ChainRecordStatus;
import com.visafe.domain.chain.enums.RecordType;
import com.visafe.domain.chain.repository.ChainRecordRepository;
import com.visafe.domain.contract.dto.response.ProcessingResponse;
import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.entity.ContractEvent;
import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.contract.enums.ContractEventType;
import com.visafe.domain.contract.enums.ProcessingTask;
import com.visafe.domain.contract.repository.ContractEventRepository;
import com.visafe.domain.translation.entity.Translation;
import com.visafe.domain.translation.enums.TranslationStatus;
import com.visafe.domain.translation.repository.TranslationRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractProcessingService {

    /** EXTRACTION_FAILED 이벤트 payload에서 실패 사유를 담는 키 */
    public static final String ERROR_PAYLOAD_KEY = "error";

    private static final Set<ContractEventType> EXTRACTION_EVENTS = EnumSet.of(
            ContractEventType.FILE_UPLOADED,
            ContractEventType.EXTRACTION_STARTED,
            ContractEventType.EXTRACTION_FAILED,
            ContractEventType.EXTRACTION_COMPLETED);

    private final ContractEventRepository contractEventRepository;
    private final TranslationRepository translationRepository;
    private final ChainRecordRepository chainRecordRepository;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 계약의 현재 stage에서 진행 중인 작업을 계산한다.
     *
     * <p>UPLOADED는 AI 추출, TRANSLATING은 번역, RECORDING은 블록체인 기록이며, 그 밖의 stage는 진행 중인 작업이 없다.
     * 상태를 따로 저장하지 않고 현재 회차의 이벤트·번역본·블록체인 기록에서 계산한다.</p>
     *
     * @param contract 진행 상태를 확인할 계약
     * @return 진행 중인 작업, 없으면 {@code null}
     */
    public ProcessingResponse resolve(Contract contract) {
        ContractVersion version = contract.getCurrentVersion();
        if (version == null) {
            return null;
        }
        return switch (contract.getStage()) {
            case UPLOADED -> extraction(version.getId());
            case TRANSLATING -> translation(version.getId());
            case RECORDING -> chainRecord(version.getId());
            default -> null;
        };
    }

    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * 마지막 원본 업로드 이후의 추출 이벤트로 AI 추출 상태를 계산한다.
     *
     * <p>마지막 추출 이벤트가 EXTRACTION_FAILED면 고용주가 다시 시도(R3)해야 하므로 FAILED다.
     * 시도 횟수는 마지막 업로드 이후의 EXTRACTION_STARTED 수다.</p>
     *
     * @param versionId 현재 회차 ID
     * @return AI 추출 진행 상태
     */
    private ProcessingResponse extraction(Long versionId) {
        List<ContractEvent> events = contractEventRepository.findVersionEvents(versionId, EXTRACTION_EVENTS);
        int attempts = 0;
        ContractEvent last = null;
        for (ContractEvent event : events) {
            if (event.getEventType() == ContractEventType.FILE_UPLOADED) {
                attempts = 0;
                last = null;
                continue;
            }
            if (event.getEventType() == ContractEventType.EXTRACTION_STARTED) {
                attempts++;
            }
            last = event;
        }
        int attempt = Math.max(attempts, 1);
        if (last != null && last.getEventType() == ContractEventType.EXTRACTION_FAILED) {
            return ProcessingResponse.failed(ProcessingTask.EXTRACTION, attempt, errorOf(last));
        }
        return ProcessingResponse.running(ProcessingTask.EXTRACTION, attempt);
    }

    /**
     * 회차의 마지막 번역본으로 번역 상태를 계산한다.
     *
     * <p>실패해도 3회까지는 서버가 자동으로 다시 시도하므로 RUNNING이고, 3회 모두 실패하면 FAILED다.</p>
     *
     * @param versionId 현재 회차 ID
     * @return 번역 진행 상태
     */
    private ProcessingResponse translation(Long versionId) {
        Optional<Translation> latest =
                translationRepository.findFirstByContractVersionIdOrderByTranslationNoDesc(versionId);
        if (latest.isEmpty()) {
            return ProcessingResponse.running(ProcessingTask.TRANSLATION, 1);
        }
        Translation translation = latest.get();
        int attempt = Math.max(translation.getAttemptCount(), 1);
        if (translation.getStatus() == TranslationStatus.FAILED && !translation.canRetry()) {
            return ProcessingResponse.failed(ProcessingTask.TRANSLATION, attempt, translation.getLastError());
        }
        return ProcessingResponse.running(ProcessingTask.TRANSLATION, attempt);
    }

    /**
     * 회차의 확정 기록으로 블록체인 기록 상태를 계산한다.
     *
     * <p>FAILED여도 서버가 retry_count를 올리며 다시 시도한다. 기록 행에는 오류 내용을 저장하지 않으므로 lastError는 비어 있다.</p>
     *
     * @param versionId 현재 회차 ID
     * @return 블록체인 기록 진행 상태
     */
    private ProcessingResponse chainRecord(Long versionId) {
        Optional<ChainRecord> record = chainRecordRepository.findFirstByContractVersionIdAndRecordTypeOrderByIdDesc(
                versionId, RecordType.CONFIRM);
        if (record.isEmpty()) {
            return ProcessingResponse.running(ProcessingTask.CHAIN_RECORD, 1);
        }
        int attempt = record.get().getRetryCount() + 1;
        if (record.get().getStatus() == ChainRecordStatus.FAILED) {
            return ProcessingResponse.failed(ProcessingTask.CHAIN_RECORD, attempt, null);
        }
        return ProcessingResponse.running(ProcessingTask.CHAIN_RECORD, attempt);
    }

    /**
     * 실패 이벤트의 payload에서 실패 사유를 꺼낸다.
     *
     * @param event EXTRACTION_FAILED 이벤트
     * @return 실패 사유, 없으면 {@code null}
     */
    private static String errorOf(ContractEvent event) {
        Object error = event.getPayload().get(ERROR_PAYLOAD_KEY);
        return error == null ? null : error.toString();
    }
}
