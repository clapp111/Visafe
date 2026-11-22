package com.visafe.domain.contract.service;

import com.visafe.domain.contract.dto.request.ContractSubmitRequest;
import com.visafe.domain.contract.dto.response.ContractStageResponse;
import com.visafe.domain.contract.dto.response.ProcessingResponse;
import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.contract.enums.ContractEventType;
import com.visafe.domain.contract.enums.ContractStage;
import com.visafe.domain.contract.enums.ProcessingStatus;
import com.visafe.domain.contract.repository.ContractVersionRepository;
import com.visafe.domain.member.entity.MemberDetails;
import com.visafe.domain.member.repository.MemberRepository;
import com.visafe.domain.term.TermItemsHash;
import com.visafe.domain.term.entity.ContractLaborTerms;
import com.visafe.domain.term.enums.TermField;
import com.visafe.domain.term.repository.ContractLaborTermsRepository;
import com.visafe.domain.translation.entity.Translation;
import com.visafe.domain.translation.repository.TranslationRepository;
import com.visafe.domain.translation.translator.TranslationRequestedEvent;
import com.visafe.global.exception.CustomErrorCode;
import com.visafe.global.exception.RestApiException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractSubmissionService {

    private final ContractAccessService contractAccessService;
    private final ContractProcessingService contractProcessingService;
    private final ContractEventRecorder contractEventRecorder;
    private final MemberRepository memberRepository;
    private final ContractLaborTermsRepository contractLaborTermsRepository;
    private final ContractVersionRepository contractVersionRepository;
    private final TranslationRepository translationRepository;
    private final ApplicationEventPublisher eventPublisher;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 검토를 마친 계약을 제출하고 번역을 시작한다.
     *
     * <p>제출 API(R6)에서 쓴다. 필수 항목이 하나라도 비어 있으면 거절하고, 최저임금 같은 경고는 제출을 막지 않는다.
     * 항목 해시를 계산해 회차를 SUBMITTED로 바꾸고 stage를 TRANSLATING으로 둔 뒤 SUBMITTED 이벤트를 남긴다.
     * 돌아온 계약을 고친 회차면 이전 회차(RETURNED)를 대체(SUPERSEDED)한다.
     * 계약의 번역 언어로 첫 번역본(PENDING)을 만들고, 커밋된 뒤 비동기로 번역한다.</p>
     *
     * @param contractId 계약 ID
     * @param request 원본 일치 확인 체크
     * @param member 로그인한 고용주
     * @return stage TRANSLATING, 번역 진행 상태
     * @throws RestApiException 계약이 없거나 볼 권한이 없는 경우, 원본 일치 확인을 체크하지 않은 경우,
     *                          stage가 REVIEWING이 아니거나 회차가 DRAFT가 아닌 경우, 또는 비어 있는 필수 항목이 있는 경우
     */
    @Transactional
    public ContractStageResponse submit(Long contractId, ContractSubmitRequest request, MemberDetails member) {
        Contract contract = contractAccessService.getAccessibleContract(contractId, member);
        if (!Boolean.TRUE.equals(request.confirmedOriginalMatch())) {
            throw new RestApiException(CustomErrorCode.CONFIRMATION_REQUIRED);
        }
        ContractVersion version = contract.getCurrentVersion();
        if (contract.getStage() != ContractStage.REVIEWING || !version.isDraft()) {
            throw new RestApiException(CustomErrorCode.CONTRACT_STAGE_CONFLICT);
        }
        ContractLaborTerms terms =
                contractLaborTermsRepository.findById(version.getId()).orElseThrow();
        checkRequired(terms);

        version.submit(memberRepository.getReferenceById(member.getMemberId()), TermItemsHash.of(terms), Instant.now());
        if (version.getVersionNo() > 1) {
            contractVersionRepository
                    .findByContractIdAndVersionNo(contract.getId(), version.getVersionNo() - 1)
                    .orElseThrow()
                    .supersede();
        }
        contract.changeStage(ContractStage.TRANSLATING);
        contractEventRecorder.record(contract, ContractEventType.SUBMITTED, member.getMemberId(), null);
        startTranslation(contract, 1);
        return stageResponse(contract);
    }

    /**
     * 최종 실패한 번역을 새 번역본으로 다시 시도한다.
     *
     * <p>번역 다시 시도 API(R7)에서 쓴다. 번역본 하나에 3회 모두 실패한 뒤에만 할 수 있으며,
     * 같은 회차에 번역본 번호를 하나 올린 새 번역본(PENDING)을 만들고 커밋된 뒤 비동기로 번역한다.</p>
     *
     * @param contractId 계약 ID
     * @param member 로그인한 고용주
     * @return stage TRANSLATING, 번역 진행 상태
     * @throws RestApiException 계약이 없거나 볼 권한이 없는 경우, 또는 stage가 TRANSLATING이 아니거나 마지막 번역이 최종 실패가 아닌 경우
     */
    @Transactional
    public ContractStageResponse retryTranslation(Long contractId, MemberDetails member) {
        Contract contract = contractAccessService.getAccessibleContract(contractId, member);
        ProcessingResponse processing = contractProcessingService.resolve(contract);
        if (contract.getStage() != ContractStage.TRANSLATING
                || processing == null
                || processing.status() != ProcessingStatus.FAILED) {
            throw new RestApiException(CustomErrorCode.CONTRACT_STAGE_CONFLICT);
        }
        Translation latest = translationRepository
                .findFirstByContractVersionIdOrderByTranslationNoDesc(
                        contract.getCurrentVersion().getId())
                .orElseThrow();
        startTranslation(contract, latest.getTranslationNo() + 1);
        return stageResponse(contract);
    }

    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * 제출 시 필수 항목(API 명세 부록 A)이 모두 채워졌는지 검사한다.
     *
     * @param terms 현재 회차의 항목 값
     * @throws RestApiException 비어 있는 필수 항목이 있는 경우 (빈 항목 키를 메시지에 담음)
     */
    private static void checkRequired(ContractLaborTerms terms) {
        List<String> missing = Arrays.stream(TermField.values())
                .filter(TermField::isRequired)
                .filter(field -> terms.getValue(field) == null)
                .map(TermField::getKey)
                .toList();
        if (!missing.isEmpty()) {
            throw new RestApiException(
                    CustomErrorCode.TERMS_REQUIRED_MISSING, "비어 있는 필수 항목이 있습니다: " + String.join(", ", missing));
        }
    }

    /**
     * 현재 회차에 새 번역본을 만들고, 커밋된 뒤 번역하도록 요청한다.
     *
     * @param contract 번역할 계약
     * @param translationNo 새 번역본 번호
     */
    private void startTranslation(Contract contract, int translationNo) {
        Translation translation = translationRepository.save(
                Translation.create(contract.getCurrentVersion(), contract.getTargetLanguage(), translationNo));
        eventPublisher.publishEvent(new TranslationRequestedEvent(contract.getId(), translation.getId()));
    }

    /**
     * stage 변경 공통 응답을 만든다.
     *
     * @param contract 계약
     * @return 현재 stage와 진행 상태
     */
    private ContractStageResponse stageResponse(Contract contract) {
        return ContractStageResponse.of(contract, contractProcessingService.resolve(contract));
    }
}
