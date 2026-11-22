package com.visafe.domain.contract.service;

import com.visafe.domain.contract.dto.request.ContractCreateRequest;
import com.visafe.domain.contract.dto.response.ContractStageResponse;
import com.visafe.domain.contract.dto.response.ProcessingResponse;
import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.entity.ContractFile;
import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.contract.enums.ContractEventType;
import com.visafe.domain.contract.enums.ContractStage;
import com.visafe.domain.contract.enums.ContractStatus;
import com.visafe.domain.contract.enums.ProcessingStatus;
import com.visafe.domain.contract.extraction.ExtractionRequestedEvent;
import com.visafe.domain.contract.repository.ContractRepository;
import com.visafe.domain.contract.repository.ContractVersionRepository;
import com.visafe.domain.member.entity.Member;
import com.visafe.domain.member.entity.MemberDetails;
import com.visafe.domain.member.entity.MemberWorkerProfile;
import com.visafe.domain.member.enums.MemberRole;
import com.visafe.domain.member.repository.MemberRepository;
import com.visafe.domain.member.repository.MemberWorkerProfileRepository;
import com.visafe.domain.term.repository.ContractLaborTermsRepository;
import com.visafe.domain.term.repository.ContractTermAnnotationRepository;
import com.visafe.global.common.Entities;
import com.visafe.global.exception.CustomErrorCode;
import com.visafe.global.exception.RestApiException;
import java.time.Year;
import java.time.ZoneId;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class ContractRegistrationService {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

    private final ContractRepository contractRepository;
    private final ContractVersionRepository contractVersionRepository;
    private final MemberRepository memberRepository;
    private final MemberWorkerProfileRepository memberWorkerProfileRepository;
    private final ContractLaborTermsRepository contractLaborTermsRepository;
    private final ContractTermAnnotationRepository contractTermAnnotationRepository;
    private final ContractAccessService contractAccessService;
    private final ContractFileService contractFileService;
    private final ContractEventRecorder contractEventRecorder;
    private final ContractProcessingService contractProcessingService;
    private final ApplicationEventPublisher eventPublisher;
    private final boolean authorityReviewEnabled;

    public ContractRegistrationService(
            ContractRepository contractRepository,
            ContractVersionRepository contractVersionRepository,
            MemberRepository memberRepository,
            MemberWorkerProfileRepository memberWorkerProfileRepository,
            ContractLaborTermsRepository contractLaborTermsRepository,
            ContractTermAnnotationRepository contractTermAnnotationRepository,
            ContractAccessService contractAccessService,
            ContractFileService contractFileService,
            ContractEventRecorder contractEventRecorder,
            ContractProcessingService contractProcessingService,
            ApplicationEventPublisher eventPublisher,
            @Value("${visafe.authority-review.enabled}") boolean authorityReviewEnabled) {
        this.contractRepository = contractRepository;
        this.contractVersionRepository = contractVersionRepository;
        this.memberRepository = memberRepository;
        this.memberWorkerProfileRepository = memberWorkerProfileRepository;
        this.contractLaborTermsRepository = contractLaborTermsRepository;
        this.contractTermAnnotationRepository = contractTermAnnotationRepository;
        this.contractAccessService = contractAccessService;
        this.contractFileService = contractFileService;
        this.contractEventRecorder = contractEventRecorder;
        this.contractProcessingService = contractProcessingService;
        this.eventPublisher = eventPublisher;
        this.authorityReviewEnabled = authorityReviewEnabled;
    }

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 계약을 만들고 원본을 저장한 뒤 AI 추출을 시작한다. (R1)
     *
     * <p>사업주는 로그인한 고용주의 사업주, 번역 언어는 근로자의 모국어, 기관 검토 여부는 서버 설정에서 정한다.
     * 1회차(DRAFT)를 만들고, 커밋된 뒤 비동기로 추출을 시작한다.</p>
     *
     * @param request 근로자와 대체할 이전 계약
     * @param upload 원본 파일
     * @param member 로그인한 고용주
     * @return stage UPLOADED, 추출 진행 상태
     * @throws RestApiException 근로자 계정이 아닌 경우, 대체할 계약이 확정 상태가 아니거나 당사자가 다른 경우,
     *                          또는 PDF·JPG·PNG가 아닌 경우
     */
    public ContractStageResponse create(ContractCreateRequest request, MultipartFile upload, MemberDetails member) {
        Member employer = memberRepository
                .findById(member.getMemberId())
                .orElseThrow(() -> new RestApiException(CustomErrorCode.MEMBER_NOT_FOUND));
        Member worker = memberRepository
                .findById(request.workerMemberId())
                .filter(found -> found.hasRole(MemberRole.WORKER))
                .orElseThrow(() -> new RestApiException(CustomErrorCode.WORKER_NOT_FOUND));
        MemberWorkerProfile profile = memberWorkerProfileRepository
                .findById(worker.getId())
                .orElseThrow(() -> new RestApiException(CustomErrorCode.WORKER_NOT_FOUND));
        Contract previous = request.previousContractId() == null
                ? null
                : findReplaceable(request.previousContractId(), employer, worker);

        Contract contract = contractRepository.save(Contract.create(
                nextContractNo(),
                employer.getCompany(),
                employer,
                worker,
                profile.getPreferredLanguage(),
                previous,
                authorityReviewEnabled));
        ContractFile file = contractFileService.saveOriginal(contract, upload, employer);
        ContractVersion version = contractVersionRepository.save(ContractVersion.create(contract, 1, file));
        contract.changeCurrentVersion(version);

        contractEventRecorder.record(contract, ContractEventType.CONTRACT_CREATED, employer.getId(), null);
        contractEventRecorder.record(
                contract, ContractEventType.FILE_UPLOADED, employer.getId(), Map.of("fileId", file.getId()));
        startExtraction(contract);
        return stageResponse(contract);
    }

    /**
     * 원본을 다시 올리고 다시 추출한다. (R2)
     *
     * <p>현재 회차가 DRAFT(stage UPLOADED·REVIEWING)일 때만 할 수 있다. 회차의 기준 파일을 바꾸고 항목 값과 부가 정보를 비운다.
     * 예전 파일과 조항은 기록 보존 원칙에 따라 남긴다.</p>
     *
     * @param contractId 계약 ID
     * @param upload 새 원본 파일
     * @param member 로그인한 고용주
     * @return stage UPLOADED, 추출 진행 상태
     * @throws RestApiException 계약이 없거나 볼 권한이 없는 경우, 지금 stage에서 할 수 없는 경우,
     *                          또는 PDF·JPG·PNG가 아닌 경우
     */
    public ContractStageResponse replaceFile(Long contractId, MultipartFile upload, MemberDetails member) {
        Contract contract = contractAccessService.getAccessibleContract(contractId, member);
        ContractVersion version = contract.getCurrentVersion();
        boolean draftStage =
                contract.getStage() == ContractStage.UPLOADED || contract.getStage() == ContractStage.REVIEWING;
        if (!draftStage || !version.isDraft()) {
            throw new RestApiException(CustomErrorCode.CONTRACT_STAGE_CONFLICT);
        }

        ContractFile file = contractFileService.saveOriginal(
                contract, upload, memberRepository.getReferenceById(member.getMemberId()));
        version.replaceFile(file);
        contractLaborTermsRepository.findById(version.getId()).ifPresent(contractLaborTermsRepository::delete);
        contractTermAnnotationRepository.deleteByContractVersionId(version.getId());
        if (contract.getStage() == ContractStage.REVIEWING) {
            contract.changeStage(ContractStage.UPLOADED);
        }

        contractEventRecorder.record(
                contract, ContractEventType.FILE_UPLOADED, member.getMemberId(), Map.of("fileId", file.getId()));
        startExtraction(contract);
        return stageResponse(contract);
    }

    /**
     * 실패한 AI 추출을 다시 시도한다. (R3)
     *
     * @param contractId 계약 ID
     * @param member 로그인한 고용주
     * @return stage UPLOADED, 추출 진행 상태
     * @throws RestApiException 계약이 없거나 볼 권한이 없는 경우, 또는 stage가 UPLOADED가 아니거나 마지막 추출이 실패하지 않은 경우
     */
    public ContractStageResponse retryExtraction(Long contractId, MemberDetails member) {
        Contract contract = contractAccessService.getAccessibleContract(contractId, member);
        ProcessingResponse processing = contractProcessingService.resolve(contract);
        if (contract.getStage() != ContractStage.UPLOADED
                || processing == null
                || processing.status() != ProcessingStatus.FAILED) {
            throw new RestApiException(CustomErrorCode.CONTRACT_STAGE_CONFLICT);
        }
        startExtraction(contract);
        return stageResponse(contract);
    }

    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * 새 계약이 대체할 이전 계약을 찾는다.
     *
     * @param previousContractId 이전 계약 ID
     * @param employer 로그인한 고용주
     * @param worker 새 계약의 근로자
     * @return 같은 사업주·근로자의 확정 계약으로, 아직 다른 계약이 대체하지 않은 것
     * @throws RestApiException 대체할 수 없는 계약인 경우
     */
    private Contract findReplaceable(Long previousContractId, Member employer, Member worker) {
        return contractRepository
                .findById(previousContractId)
                .filter(previous -> previous.getStatus() == ContractStatus.CONFIRMED)
                .filter(previous -> Entities.isSame(previous.getCompany(), employer.getCompany(), c -> c.getId()))
                .filter(previous -> Entities.isSame(previous.getWorker(), worker, Member::getId))
                .filter(previous -> !contractRepository.existsByPreviousContractId(previous.getId()))
                .orElseThrow(() -> new RestApiException(CustomErrorCode.PREVIOUS_CONTRACT_INVALID));
    }

    /**
     * 내부 계약 번호를 발급한다. (C-연도-일련번호 5자리, 연도는 한국 시간 기준)
     *
     * @return 새 계약 번호
     */
    private String nextContractNo() {
        return "C-%d-%05d".formatted(Year.now(KOREA).getValue(), contractRepository.nextContractNoSequence());
    }

    /**
     * 추출 시작을 이력에 남기고, 커밋된 뒤 현재 회차의 원본으로 추출하도록 요청한다.
     *
     * @param contract 추출할 계약
     */
    private void startExtraction(Contract contract) {
        ContractVersion version = contract.getCurrentVersion();
        contractEventRecorder.recordBySystem(contract, ContractEventType.EXTRACTION_STARTED, null);
        eventPublisher.publishEvent(new ExtractionRequestedEvent(
                contract.getId(), version.getId(), version.getContractFile().getId()));
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
