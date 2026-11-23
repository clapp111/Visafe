package com.visafe.domain.authority.service;

import com.visafe.domain.authority.dto.request.AuthorityRejectRequest;
import com.visafe.domain.authority.entity.AuthorityReview;
import com.visafe.domain.authority.repository.AuthorityReviewRepository;
import com.visafe.domain.chain.service.ContractConfirmationService;
import com.visafe.domain.contract.dto.response.ContractStageResponse;
import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.contract.enums.ContractEventType;
import com.visafe.domain.contract.enums.ContractStage;
import com.visafe.domain.contract.service.ContractAccessService;
import com.visafe.domain.contract.service.ContractEventRecorder;
import com.visafe.domain.contract.service.ContractProcessingService;
import com.visafe.domain.member.entity.Member;
import com.visafe.domain.member.entity.MemberDetails;
import com.visafe.domain.member.repository.MemberRepository;
import com.visafe.domain.notification.enums.NotificationType;
import com.visafe.domain.notification.service.NotificationPublisher;
import com.visafe.domain.review.entity.ReviewResponse;
import com.visafe.domain.review.enums.ReviewAction;
import com.visafe.domain.review.repository.ReviewResponseRepository;
import com.visafe.global.exception.CustomErrorCode;
import com.visafe.global.exception.RestApiException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorityReviewService {

    private final ContractAccessService contractAccessService;
    private final ContractProcessingService contractProcessingService;
    private final ContractEventRecorder contractEventRecorder;
    private final ContractConfirmationService contractConfirmationService;
    private final NotificationPublisher notificationPublisher;
    private final AuthorityReviewRepository authorityReviewRepository;
    private final ReviewResponseRepository reviewResponseRepository;
    private final MemberRepository memberRepository;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 기관 검토 중인 계약을 승인하고 블록체인 기록을 시작한다.
     *
     * <p>확정 API(D3)에서 쓴다. 승인 기록을 남기고 AUTHORITY_APPROVED 이벤트와 고용주 알림을 남긴 뒤,
     * 근로자의 "이해했습니다" 응답으로 확정 기록을 시작한다. 이후 과정은 근로자가 이해했다고 응답한 경우와 같다.
     * 이력 해시에는 AUTHORITY_APPROVED까지 들어간다.</p>
     *
     * @param contractId 계약 ID
     * @param member 로그인한 관리자
     * @return stage RECORDING, 블록체인 기록 진행 상태
     * @throws RestApiException 계약이 없는 경우, stage가 AUTHORITY_REVIEW가 아닌 경우, 또는 이미 검토한 회차인 경우
     */
    @Transactional
    public ContractStageResponse approve(Long contractId, MemberDetails member) {
        Contract contract = reviewableContract(contractId, member);
        ContractVersion version = contract.getCurrentVersion();
        ReviewResponse understood = reviewResponseRepository
                .findFirstByTranslationContractVersionIdAndActionOrderByIdDesc(version.getId(), ReviewAction.UNDERSTOOD)
                .orElseThrow();

        AuthorityReview review =
                authorityReviewRepository.save(AuthorityReview.approve(version, reviewer(member), null));
        contractEventRecorder.record(
                contract,
                ContractEventType.AUTHORITY_APPROVED,
                member.getMemberId(),
                Map.of("authorityReviewId", review.getId()));
        notificationPublisher.publish(contract.getEmployer(), contract, NotificationType.AUTHORITY_REVIEWED);
        contractConfirmationService.startRecording(contract, understood);
        return ContractStageResponse.of(contract, contractProcessingService.resolve(contract));
    }

    /**
     * 기관 검토 중인 계약을 반려해 고용주에게 돌려보낸다.
     *
     * <p>반려 API(D4)에서 쓴다. 반려 기록(사유 포함)을 남기고 회차와 stage를 RETURNED로 바꾼 뒤
     * AUTHORITY_REJECTED 이벤트와 고용주 알림을 남긴다. 고용주는 돌아온 계약 수정(R8)으로 다시 고친다.</p>
     *
     * @param contractId 계약 ID
     * @param request 반려 사유
     * @param member 로그인한 관리자
     * @return stage RETURNED
     * @throws RestApiException 반려 사유가 비어 있는 경우, 계약이 없는 경우, stage가 AUTHORITY_REVIEW가 아닌 경우,
     *                          또는 이미 검토한 회차인 경우
     */
    @Transactional
    public ContractStageResponse reject(Long contractId, AuthorityRejectRequest request, MemberDetails member) {
        if (request.reason() == null || request.reason().isBlank()) {
            throw new RestApiException(CustomErrorCode.REJECT_REASON_REQUIRED);
        }
        Contract contract = reviewableContract(contractId, member);
        ContractVersion version = contract.getCurrentVersion();
        String reason = request.reason().strip();

        AuthorityReview review =
                authorityReviewRepository.save(AuthorityReview.reject(version, reviewer(member), reason));
        version.markReturned();
        contract.changeStage(ContractStage.RETURNED);
        contractEventRecorder.record(
                contract,
                ContractEventType.AUTHORITY_REJECTED,
                member.getMemberId(),
                Map.of("authorityReviewId", review.getId(), "reason", reason));
        notificationPublisher.publish(contract.getEmployer(), contract, NotificationType.AUTHORITY_REVIEWED);
        return ContractStageResponse.of(contract, contractProcessingService.resolve(contract));
    }

    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * 기관이 검토할 수 있는 계약을 찾는다.
     *
     * @param contractId 계약 ID
     * @param member 로그인한 관리자
     * @return stage AUTHORITY_REVIEW이고 현재 회차를 아직 검토하지 않은 계약
     * @throws RestApiException 계약이 없는 경우, stage가 AUTHORITY_REVIEW가 아닌 경우, 또는 이미 검토한 회차인 경우
     */
    private Contract reviewableContract(Long contractId, MemberDetails member) {
        Contract contract = contractAccessService.getAccessibleContract(contractId, member);
        if (contract.getStage() != ContractStage.AUTHORITY_REVIEW) {
            throw new RestApiException(CustomErrorCode.CONTRACT_STAGE_CONFLICT);
        }
        if (authorityReviewRepository.existsByContractVersionId(
                contract.getCurrentVersion().getId())) {
            throw new RestApiException(CustomErrorCode.AUTHORITY_ALREADY_REVIEWED);
        }
        return contract;
    }

    /**
     * 검토한 관리자를 불러온다.
     *
     * @param member 로그인한 관리자
     * @return 관리자 회원 (역할 확인용으로 읽음)
     */
    private Member reviewer(MemberDetails member) {
        return memberRepository.findById(member.getMemberId()).orElseThrow();
    }
}
