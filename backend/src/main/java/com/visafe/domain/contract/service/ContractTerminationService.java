package com.visafe.domain.contract.service;

import com.visafe.domain.contract.dto.request.TerminationCreateRequest;
import com.visafe.domain.contract.dto.request.TerminationRespondRequest;
import com.visafe.domain.contract.dto.response.TerminationResponse;
import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.entity.ContractTermination;
import com.visafe.domain.contract.enums.ContractEventType;
import com.visafe.domain.contract.enums.ContractStage;
import com.visafe.domain.contract.enums.TerminationDecision;
import com.visafe.domain.contract.enums.TerminationReasonType;
import com.visafe.domain.contract.enums.TerminationStatus;
import com.visafe.domain.contract.repository.ContractRepository;
import com.visafe.domain.contract.repository.ContractTerminationRepository;
import com.visafe.domain.member.entity.Member;
import com.visafe.domain.member.entity.MemberDetails;
import com.visafe.domain.member.repository.MemberRepository;
import com.visafe.domain.notification.enums.NotificationType;
import com.visafe.domain.notification.service.NotificationPublisher;
import com.visafe.domain.term.enums.TermField;
import com.visafe.domain.term.repository.ContractLaborTermsRepository;
import com.visafe.global.common.Entities;
import com.visafe.global.exception.CustomErrorCode;
import com.visafe.global.exception.RestApiException;
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
public class ContractTerminationService {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

    private final ContractAccessService contractAccessService;
    private final ContractEventRecorder contractEventRecorder;
    private final NotificationPublisher notificationPublisher;
    private final ContractRepository contractRepository;
    private final ContractTerminationRepository contractTerminationRepository;
    private final ContractLaborTermsRepository contractLaborTermsRepository;
    private final MemberRepository memberRepository;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 확정된 계약의 합의 종료를 요청한다.
     *
     * <p>종료 요청 API(M4)에서 쓴다. 계약을 등록한 고용주와 근로자만 요청할 수 있다. 종료일은 마지막 근무일로,
     * 오늘(한국 날짜)부터 계약 종료일까지만 고를 수 있다. 새 계약으로 대체되는 중인 계약은 요청할 수 없다.
     * 요청을 저장하고 TERMINATION_REQUESTED 이벤트를 남긴 뒤 상대방에게 알린다.</p>
     *
     * @param contractId 계약 ID
     * @param request 종료일과 사유
     * @param member 로그인한 당사자
     * @return 저장한 종료 요청
     * @throws RestApiException 계약이 없거나 볼 권한이 없는 경우, 계약 당사자가 아닌 경우,
     *                          계약이 확정 상태가 아니거나 새 계약으로 대체되는 중인 경우, 진행 중인 종료가 있는 경우,
     *                          또는 종료일이 오늘 이전이거나 계약 종료일 이후인 경우
     */
    @Transactional
    public TerminationResponse request(Long contractId, TerminationCreateRequest request, MemberDetails member) {
        Contract contract = contractAccessService.getAccessibleContract(contractId, member);
        Member requester = memberRepository.getReferenceById(member.getMemberId());
        if (!contract.isParty(requester)) {
            throw new RestApiException(CustomErrorCode.TERMINATION_NOT_ALLOWED);
        }
        if (contract.getStage() != ContractStage.DONE || contractRepository.existsByPreviousContractId(contractId)) {
            throw new RestApiException(CustomErrorCode.CONTRACT_STAGE_CONFLICT);
        }
        boolean active = contractTerminationRepository
                .findFirstByContractIdAndStatusInOrderByIdDesc(
                        contractId, EnumSet.of(TerminationStatus.REQUESTED, TerminationStatus.AGREED))
                .isPresent();
        if (active) {
            throw new RestApiException(CustomErrorCode.TERMINATION_ALREADY_ACTIVE);
        }
        checkEndDate(contract, request.endDate());

        ContractTermination termination = contractTerminationRepository.save(ContractTermination.requestMutual(
                contract, requester, request.reasonText(), request.endDate(), Instant.now()));
        contractEventRecorder.record(
                contract,
                ContractEventType.TERMINATION_REQUESTED,
                member.getMemberId(),
                Map.of(
                        "terminationId",
                        termination.getId(),
                        "endDate",
                        request.endDate().toString()));
        notificationPublisher.publish(
                counterpart(contract, requester), contract, NotificationType.TERMINATION_REQUESTED);
        return TerminationResponse.of(termination);
    }

    /**
     * 합의 종료 요청에 동의하거나 거절한다.
     *
     * <p>종료 동의·거절 API(M5)에서 쓴다. 요청한 사람의 상대 당사자만 응답할 수 있다.
     * 동의하면 AGREED가 되고 종료일이 지나면 스케줄러가 계약을 끝낸다. 거절하면 DECLINED가 된다.
     * 어느 쪽이든 이벤트를 남기고 요청자에게 알린다.</p>
     *
     * @param contractId 계약 ID
     * @param terminationId 종료 ID
     * @param request 동의 여부
     * @param member 로그인한 당사자
     * @return 응답한 종료 요청
     * @throws RestApiException 계약이나 종료 요청이 없거나 볼 권한이 없는 경우, 요청자 본인이거나 당사자가 아닌 경우,
     *                          또는 이미 응답·취소된 요청인 경우
     */
    @Transactional
    public TerminationResponse respond(
            Long contractId, Long terminationId, TerminationRespondRequest request, MemberDetails member) {
        Contract contract = contractAccessService.getAccessibleContract(contractId, member);
        ContractTermination termination = findTermination(contract, terminationId);
        Member responder = memberRepository.getReferenceById(member.getMemberId());
        if (!contract.isParty(responder) || isRequester(termination, member)) {
            throw new RestApiException(CustomErrorCode.TERMINATION_NOT_ALLOWED);
        }
        requireRequested(termination);

        boolean agree = request.decision() == TerminationDecision.AGREE;
        if (agree) {
            termination.agree(responder, Instant.now());
        } else {
            termination.decline(responder, Instant.now());
        }
        contractEventRecorder.record(
                contract,
                agree ? ContractEventType.TERMINATION_AGREED : ContractEventType.TERMINATION_DECLINED,
                member.getMemberId(),
                Map.of("terminationId", termination.getId()));
        notificationPublisher.publish(termination.getRequestedBy(), contract, NotificationType.TERMINATION_RESPONDED);
        return TerminationResponse.of(termination);
    }

    /**
     * 응답을 기다리는 합의 종료 요청을 취소한다.
     *
     * <p>종료 요청 취소 API(M6)에서 쓴다. 요청한 사람만 취소할 수 있으며 TERMINATION_CANCELED 이벤트를 남긴다.</p>
     *
     * @param contractId 계약 ID
     * @param terminationId 종료 ID
     * @param member 로그인한 요청자
     * @return 취소한 종료 요청
     * @throws RestApiException 계약이나 종료 요청이 없거나 볼 권한이 없는 경우, 요청자가 아닌 경우,
     *                          또는 이미 응답·취소된 요청인 경우
     */
    @Transactional
    public TerminationResponse cancel(Long contractId, Long terminationId, MemberDetails member) {
        Contract contract = contractAccessService.getAccessibleContract(contractId, member);
        ContractTermination termination = findTermination(contract, terminationId);
        if (!isRequester(termination, member)) {
            throw new RestApiException(CustomErrorCode.TERMINATION_NOT_ALLOWED);
        }
        requireRequested(termination);

        termination.cancel();
        contractEventRecorder.record(
                contract,
                ContractEventType.TERMINATION_CANCELED,
                member.getMemberId(),
                Map.of("terminationId", termination.getId()));
        return TerminationResponse.of(termination);
    }

    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * 종료일이 오늘(한국 날짜)부터 계약 종료일 사이인지 검사한다. 계약 종료일이 없으면 오늘 이후인지만 본다.
     *
     * @param contract 계약
     * @param endDate 요청한 종료일
     * @throws RestApiException 종료일이 오늘 이전이거나 계약 종료일 이후인 경우
     */
    private void checkEndDate(Contract contract, LocalDate endDate) {
        Object contractEnd = contractLaborTermsRepository
                .findById(contract.getCurrentVersion().getId())
                .map(terms -> terms.getValue(TermField.END_DATE))
                .orElse(null);
        boolean beforeToday = endDate.isBefore(LocalDate.now(KOREA));
        boolean afterContractEnd = contractEnd instanceof LocalDate end && endDate.isAfter(end);
        if (beforeToday || afterContractEnd) {
            throw new RestApiException(CustomErrorCode.TERMINATION_DATE_INVALID);
        }
    }

    /**
     * 계약에 속한 종료를 찾는다.
     *
     * @param contract 계약
     * @param terminationId 종료 ID
     * @return 종료
     * @throws RestApiException 계약에 속한 종료가 아닌 경우
     */
    private ContractTermination findTermination(Contract contract, Long terminationId) {
        return contractTerminationRepository
                .findById(terminationId)
                .filter(termination -> Entities.isSame(termination.getContract(), contract, Contract::getId))
                .orElseThrow(() -> new RestApiException(CustomErrorCode.CONTRACT_NOT_FOUND, "종료 요청을 찾을 수 없습니다."));
    }

    /**
     * 응답을 기다리는 합의 종료인지 확인한다.
     *
     * @param termination 종료
     * @throws RestApiException 이미 응답·취소됐거나 서버가 만든 종료(기간 만료·대체)인 경우
     */
    private static void requireRequested(ContractTermination termination) {
        if (termination.getReasonType() != TerminationReasonType.MUTUAL
                || termination.getStatus() != TerminationStatus.REQUESTED) {
            throw new RestApiException(CustomErrorCode.TERMINATION_STATUS_CONFLICT);
        }
    }

    /**
     * 로그인한 사람이 종료를 요청한 사람인지 확인한다.
     *
     * @param termination 종료
     * @param member 로그인한 회원
     * @return 요청자면 {@code true}, 그렇지 않으면 {@code false}
     */
    private static boolean isRequester(ContractTermination termination, MemberDetails member) {
        return termination.getRequestedBy() != null
                && termination.getRequestedBy().getId().equals(member.getMemberId());
    }

    /**
     * 요청자의 상대 당사자를 찾는다.
     *
     * @param contract 계약
     * @param requester 요청자
     * @return 요청자가 근로자면 고용주, 고용주면 근로자
     */
    private static Member counterpart(Contract contract, Member requester) {
        return Entities.isSame(contract.getWorker(), requester, Member::getId)
                ? contract.getEmployer()
                : contract.getWorker();
    }
}
