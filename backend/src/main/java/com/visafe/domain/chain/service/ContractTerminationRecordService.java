package com.visafe.domain.chain.service;

import com.visafe.domain.chain.ChainRecordHash;
import com.visafe.domain.chain.entity.ChainRecord;
import com.visafe.domain.chain.enums.RecordType;
import com.visafe.domain.chain.recorder.ChainRecordRequestedEvent;
import com.visafe.domain.chain.repository.ChainRecordRepository;
import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.entity.ContractTermination;
import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.contract.enums.ContractEventType;
import com.visafe.domain.contract.repository.ContractEventRepository;
import com.visafe.domain.contract.service.ContractEventRecorder;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractTerminationRecordService {

    private final ContractEventRepository contractEventRepository;
    private final ChainRecordRepository chainRecordRepository;
    private final ContractEventRecorder contractEventRecorder;
    private final ApplicationEventPublisher eventPublisher;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 종료된 계약의 종료 기록을 만들어 블록체인 기록을 시작한다.
     *
     * <p>계약을 ENDED로 바꾸고 TERMINATED 이벤트를 남긴 뒤 호출한다. 이력 해시는 지금까지의 이벤트로 계산하므로 TERMINATED가 포함된다.
     * 확정 기록과 같은 조회 키에 이전 확정 기록의 해시, 종료 사유·종료일을 묶은 종료 기록(PENDING)을 저장하고
     * CHAIN_RECORD_REQUESTED 이벤트를 남긴 뒤, 커밋된 뒤 비동기로 블록체인에 보낸다.</p>
     *
     * @param contract 종료된 계약
     * @param termination 계약을 끝낸 종료 (AGREED)
     */
    @Transactional
    public void startRecording(Contract contract, ContractTermination termination) {
        ContractVersion version = contract.getCurrentVersion();
        ChainRecord confirmRecord = chainRecordRepository
                .findFirstByContractVersionIdAndRecordTypeOrderByIdDesc(version.getId(), RecordType.CONFIRM)
                .orElseThrow();
        String eventsHash =
                ChainRecordHash.eventsHash(contractEventRepository.findSnapshotsByContractId(contract.getId()));
        String anchorKey = ChainRecordHash.anchorKey(contract.getConfirmationNo());
        String nonce = ChainRecordHash.newNonce();
        String recordHash = ChainRecordHash.terminateRecordHash(
                anchorKey,
                termination.getEndDate(),
                eventsHash,
                confirmRecord.getRecordHash(),
                termination.getReasonType(),
                ChainRecordHash.workerId(contract.getWorker().getId(), nonce));

        ChainRecord record = chainRecordRepository.save(ChainRecord.terminateBuilder()
                .contractVersion(version)
                .recordHash(recordHash)
                .anchorKey(anchorKey)
                .nonce(nonce)
                .eventsHash(eventsHash)
                .contractTermination(termination)
                .previousChainRecord(confirmRecord)
                .build());
        contractEventRecorder.recordBySystem(
                contract,
                ContractEventType.CHAIN_RECORD_REQUESTED,
                Map.of("chainRecordId", record.getId(), "recordHash", recordHash, "recordType", "TERMINATE"));
        eventPublisher.publishEvent(new ChainRecordRequestedEvent(contract.getId(), record.getId()));
    }
}
