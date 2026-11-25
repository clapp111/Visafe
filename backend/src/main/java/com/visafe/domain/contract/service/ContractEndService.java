package com.visafe.domain.contract.service;

import com.visafe.domain.chain.service.ContractTerminationRecordService;
import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.entity.ContractTermination;
import com.visafe.domain.contract.enums.ContractEventType;
import com.visafe.domain.contract.enums.ContractStage;
import com.visafe.domain.contract.enums.TerminationReasonType;
import com.visafe.domain.contract.enums.TerminationStatus;
import com.visafe.domain.contract.repository.ContractRepository;
import com.visafe.domain.contract.repository.ContractTerminationRepository;
import com.visafe.domain.notification.enums.NotificationType;
import com.visafe.domain.notification.service.NotificationPublisher;
import com.visafe.domain.term.entity.ContractLaborTerms;
import com.visafe.domain.term.enums.TermField;
import com.visafe.domain.term.repository.ContractLaborTermsRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractEndService {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

    private final ContractRepository contractRepository;
    private final ContractTerminationRepository contractTerminationRepository;
    private final ContractLaborTermsRepository contractLaborTermsRepository;
    private final ContractEventRecorder contractEventRecorder;
    private final NotificationPublisher notificationPublisher;
    private final ContractTerminationRecordService contractTerminationRecordService;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 종료일이 지난 합의 종료로 계약을 끝낸다.
     *
     * <p>스케줄러가 종료 건마다 따로 호출한다. 그사이 상태가 바뀌었으면 아무것도 하지 않는다.</p>
     *
     * @param terminationId 합의 종료 ID
     * @param today 오늘 (한국 날짜)
     */
    @Transactional
    public void endAgreed(Long terminationId, LocalDate today) {
        ContractTermination termination =
                contractTerminationRepository.findById(terminationId).orElseThrow();
        Contract contract = termination.getContract();
        boolean due = termination.getReasonType() == TerminationReasonType.MUTUAL
                && termination.getStatus() == TerminationStatus.AGREED
                && termination.getEndDate().isBefore(today)
                && contract.getStage() == ContractStage.DONE;
        if (due) {
            end(contract, termination);
        }
    }

    /**
     * 계약 종료일이 지난 계약을 기간 만료로 끝낸다.
     *
     * <p>스케줄러가 계약마다 따로 호출한다. 응답을 기다리는 합의 종료 요청이 남아 있으면 서버가 취소한 뒤
     * 기간 만료 종료(EXPIRED, AGREED)를 만들어 계약을 끝낸다. 그사이 상태가 바뀌었으면 아무것도 하지 않는다.</p>
     *
     * @param contractId 계약 ID
     * @param today 오늘 (한국 날짜)
     */
    @Transactional
    public void endExpired(Long contractId, LocalDate today) {
        Contract contract = contractRepository.findById(contractId).orElseThrow();
        ContractLaborTerms terms = contractLaborTermsRepository
                .findById(contract.getCurrentVersion().getId())
                .orElseThrow();
        if (contract.getStage() != ContractStage.DONE
                || !(terms.getValue(TermField.END_DATE) instanceof LocalDate endDate)
                || !endDate.isBefore(today)) {
            return;
        }
        contractTerminationRepository
                .findFirstByContractIdAndStatusInOrderByIdDesc(
                        contractId, EnumSet.of(TerminationStatus.REQUESTED, TerminationStatus.AGREED))
                .filter(active -> active.getStatus() == TerminationStatus.REQUESTED)
                .ifPresent(requested -> {
                    requested.cancel();
                    contractEventRecorder.recordBySystem(
                            contract,
                            ContractEventType.TERMINATION_CANCELED,
                            Map.of("terminationId", requested.getId(), "reason", "EXPIRED"));
                });
        contractTerminationRepository.flush();
        end(contract, contractTerminationRepository.save(ContractTermination.expired(contract, endDate)));
    }

    /**
     * 새 계약이 확정될 때 대체되는 이전 계약을 끝낸다.
     *
     * <p>새 계약을 확정하는 트랜잭션에서 새 계약을 DONE으로 바꾸기 전에 호출한다. 종료일은 오늘(한국 날짜)이다.
     * 이전 계약에는 진행 중인 종료가 없다. (대체 계약 등록과 종료 요청에서 막음)</p>
     *
     * @param previous 대체되는 이전 계약 (DONE)
     * @param replacement 새로 확정되는 계약
     */
    @Transactional
    public void endReplaced(Contract previous, Contract replacement) {
        ContractTermination termination = contractTerminationRepository.save(ContractTermination.replaced(
                previous, LocalDate.now(KOREA), "계약 " + replacement.getContractNo() + "(으)로 대체"));
        end(previous, termination);
        contractRepository.flush();
    }

    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * 계약을 ENDED로 바꾸고 양쪽에 알린 뒤 종료 기록을 블록체인에 올리기 시작한다.
     *
     * @param contract 끝낼 계약 (DONE)
     * @param termination 계약을 끝내는 종료 (AGREED)
     */
    private void end(Contract contract, ContractTermination termination) {
        contract.terminate(Instant.now());
        contractEventRecorder.recordBySystem(
                contract,
                ContractEventType.TERMINATED,
                Map.of(
                        "terminationId",
                        termination.getId(),
                        "reasonType",
                        termination.getReasonType().name()));
        notificationPublisher.publish(contract.getEmployer(), contract, NotificationType.CONTRACT_TERMINATED);
        notificationPublisher.publish(contract.getWorker(), contract, NotificationType.CONTRACT_TERMINATED);
        contractTerminationRecordService.startRecording(contract, termination);
    }
}
