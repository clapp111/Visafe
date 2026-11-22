package com.visafe.domain.chain.service;

import com.visafe.domain.chain.ChainRecordHash;
import com.visafe.domain.chain.ConfirmationNumber;
import com.visafe.domain.chain.entity.ChainRecord;
import com.visafe.domain.chain.recorder.ChainRecordRequestedEvent;
import com.visafe.domain.chain.repository.ChainRecordRepository;
import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.contract.enums.ContractEventType;
import com.visafe.domain.contract.repository.ContractEventRepository;
import com.visafe.domain.contract.repository.ContractRepository;
import com.visafe.domain.contract.service.ContractEventRecorder;
import com.visafe.domain.review.entity.ReviewResponse;
import com.visafe.domain.translation.entity.Translation;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractConfirmationService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int NONCE_BYTES = 32;

    private final ContractRepository contractRepository;
    private final ContractEventRepository contractEventRepository;
    private final ChainRecordRepository chainRecordRepository;
    private final ContractEventRecorder contractEventRecorder;
    private final ApplicationEventPublisher eventPublisher;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 확인번호를 정하고 확정 기록을 만들어 블록체인 기록을 시작한다.
     *
     * <p>근로자가 "이해했습니다"로 응답한 뒤(기관 검토를 거치면 기관이 승인한 뒤) 호출한다.
     * 이력 해시는 지금까지의 이벤트로 계산하므로 마지막 이벤트(WORKER_UNDERSTOOD 등)를 먼저 남긴 뒤 호출해야 한다.
     * 확인번호와 함께 stage를 RECORDING으로 바꾸고, 원본 파일·항목·번역·이력 해시를 묶은 확정 기록(PENDING)을 저장한 뒤
     * CHAIN_RECORD_REQUESTED 이벤트를 남긴다. 커밋된 뒤 비동기로 블록체인에 보낸다.</p>
     *
     * @param contract 확정할 계약
     * @param understood 근로자의 "이해했습니다" 응답
     */
    @Transactional
    public void startRecording(Contract contract, ReviewResponse understood) {
        ContractVersion version = contract.getCurrentVersion();
        Translation translation = understood.getTranslation();
        String eventsHash =
                ChainRecordHash.eventsHash(contractEventRepository.findSnapshotsByContractId(contract.getId()));
        String confirmationNo = newConfirmationNo();
        String anchorKey = ChainRecordHash.anchorKey(confirmationNo);
        String nonce = newNonce();
        String fileHash = version.getContractFile().getFileHash();
        String recordHash = ChainRecordHash.confirmRecordHash(
                anchorKey,
                eventsHash,
                fileHash,
                version.getItemsHash(),
                translation.getTranslationHash(),
                ChainRecordHash.workerId(contract.getWorker().getId(), nonce));

        contract.startRecording(confirmationNo);
        ChainRecord record = chainRecordRepository.save(ChainRecord.confirmBuilder()
                .contractVersion(version)
                .recordHash(recordHash)
                .anchorKey(anchorKey)
                .nonce(nonce)
                .fileHash(fileHash)
                .itemsHash(version.getItemsHash())
                .translation(translation)
                .translationHash(translation.getTranslationHash())
                .eventsHash(eventsHash)
                .reviewResponse(understood)
                .build());
        contractEventRecorder.recordBySystem(
                contract,
                ContractEventType.CHAIN_RECORD_REQUESTED,
                Map.of("chainRecordId", record.getId(), "recordHash", recordHash));
        eventPublisher.publishEvent(new ChainRecordRequestedEvent(contract.getId(), record.getId()));
    }

    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * 다른 계약과 겹치지 않는 확인번호를 만든다.
     *
     * @return 새 확인번호
     */
    private String newConfirmationNo() {
        String confirmationNo;
        do {
            confirmationNo = ConfirmationNumber.generate();
        } while (contractRepository.existsByConfirmationNo(confirmationNo));
        return confirmationNo;
    }

    /**
     * 근로자 식별값에 섞을 32바이트 무작위값을 만든다.
     *
     * @return 0x + 소문자 16진수 64자리
     */
    private static String newNonce() {
        byte[] bytes = new byte[NONCE_BYTES];
        RANDOM.nextBytes(bytes);
        return "0x" + HexFormat.of().formatHex(bytes);
    }
}
