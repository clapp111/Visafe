package com.visafe.domain.chain.recorder;

import com.visafe.domain.chain.entity.ChainRecord;
import com.visafe.domain.chain.enums.ChainRecordStatus;
import com.visafe.domain.chain.enums.RecordType;
import com.visafe.domain.chain.repository.ChainRecordRepository;
import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.enums.ContractEventType;
import com.visafe.domain.contract.enums.ContractStage;
import com.visafe.domain.contract.repository.ContractRepository;
import com.visafe.domain.contract.service.ContractEndService;
import com.visafe.domain.contract.service.ContractEventRecorder;
import com.visafe.domain.notification.enums.NotificationType;
import com.visafe.domain.notification.service.NotificationPublisher;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 기록을 시작한 트랜잭션이 커밋된 뒤 기록을 블록체인에 보낸다.
 * 확정 기록(CONFIRM)이 블록에 포함되면 계약을 확정하고, 이 계약이 대체하는 이전 계약이 있으면 먼저 그 계약을 끝낸다.
 * 종료 기록(TERMINATE)은 계약을 이미 끝낸 뒤 올리므로 블록에 포함되면 기록만 바꾼다.
 * <p>
 * 블록체인 호출은 오래 걸리므로 트랜잭션 밖에서 하고, 결과 저장만 짧은 트랜잭션으로 한다.
 * 실패하면 기록을 FAILED로 두고 재시도 횟수를 올린다. 자동 재시도는 실제 네트워크 연동(#21)에서 넣는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChainRecordWorker {

    private final ContractRepository contractRepository;
    private final ChainRecordRepository chainRecordRepository;
    private final ContractEventRecorder contractEventRecorder;
    private final NotificationPublisher notificationPublisher;
    private final ChainRecorder chainRecorder;
    private final ContractEndService contractEndService;
    private final TransactionTemplate transactionTemplate;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChainRecordRequested(ChainRecordRequestedEvent event) {
        record(event);
    }

    /** 기록을 보낸다. 지난 요청이면 아무것도 하지 않는다. */
    public void record(ChainRecordRequestedEvent event) {
        Target target = transactionTemplate.execute(status -> loadTarget(event));
        if (target == null) {
            log.info(
                    "[블록체인 기록] 보낼 기록이 아님 - contractId={}, chainRecordId={}", event.contractId(), event.chainRecordId());
            return;
        }
        try {
            ChainReceipt receipt = chainRecorder.record(target.anchorKey(), target.recordType(), target.recordHash());
            transactionTemplate.executeWithoutResult(status -> saveReceipt(event, receipt));
        } catch (RuntimeException e) {
            log.warn("[블록체인 기록] 실패 - contractId={}, chainRecordId={}", event.contractId(), event.chainRecordId(), e);
            transactionTemplate.executeWithoutResult(status -> recordFailure(event));
        }
    }

    /** 보낼 조회 키와 묶음 해시를 읽는다. 지금 보낼 전송 대기 기록이 아니면 null. */
    private Target loadTarget(ChainRecordRequestedEvent event) {
        Contract contract = contractRepository.findById(event.contractId()).orElseThrow();
        ChainRecord record =
                chainRecordRepository.findById(event.chainRecordId()).orElseThrow();
        if (!isPending(contract, record)) {
            return null;
        }
        return new Target(record.getAnchorKey(), record.getRecordType(), record.getRecordHash());
    }

    private void saveReceipt(ChainRecordRequestedEvent event, ChainReceipt receipt) {
        Contract contract = contractRepository.findById(event.contractId()).orElseThrow();
        ChainRecord record =
                chainRecordRepository.findById(event.chainRecordId()).orElseThrow();
        if (!isPending(contract, record)) {
            log.info(
                    "[블록체인 기록] 지난 요청의 결과를 버림 - contractId={}, chainRecordId={}",
                    event.contractId(),
                    event.chainRecordId());
            return;
        }
        Instant now = Instant.now();
        record.markSent(receipt.txHash(), now);
        record.markMined(receipt.blockNumber(), now);
        if (record.getRecordType() == RecordType.TERMINATE) {
            return;
        }
        if (contract.getPreviousContract() != null) {
            contractEndService.endReplaced(contract.getPreviousContract(), contract);
        }
        contract.confirm(now);
        contract.getCurrentVersion().confirm();
        contractEventRecorder.recordBySystem(
                contract,
                ContractEventType.CONFIRMED,
                Map.of(
                        "chainRecordId", record.getId(),
                        "txHash", receipt.txHash(),
                        "confirmationNo", contract.getConfirmationNo()));
        notificationPublisher.publish(contract.getEmployer(), contract, NotificationType.CONTRACT_CONFIRMED);
        notificationPublisher.publish(contract.getWorker(), contract, NotificationType.CONTRACT_CONFIRMED);
    }

    private void recordFailure(ChainRecordRequestedEvent event) {
        Contract contract = contractRepository.findById(event.contractId()).orElseThrow();
        ChainRecord record =
                chainRecordRepository.findById(event.chainRecordId()).orElseThrow();
        if (!isPending(contract, record)) {
            return;
        }
        record.markFailed();
        contractEventRecorder.recordBySystem(
                contract,
                ContractEventType.CHAIN_RECORD_FAILED,
                Map.of("chainRecordId", record.getId(), "retryCount", record.getRetryCount()));
    }

    /** 이 기록이 현재 회차의 전송 대기 기록이고 계약이 그 기록을 기다리는 stage인지. (확정 기록은 RECORDING, 종료 기록은 ENDED) */
    private static boolean isPending(Contract contract, ChainRecord record) {
        ContractStage waitingStage =
                record.getRecordType() == RecordType.CONFIRM ? ContractStage.RECORDING : ContractStage.ENDED;
        return contract.getStage() == waitingStage
                && record.getStatus() == ChainRecordStatus.PENDING
                && record.getContractVersion()
                        .getId()
                        .equals(contract.getCurrentVersion().getId());
    }

    private record Target(String anchorKey, RecordType recordType, String recordHash) {}
}
