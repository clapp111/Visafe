package com.visafe.domain.contract.service;

import com.visafe.domain.contract.dto.response.ContractStageResponse;
import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.contract.enums.ContractStage;
import com.visafe.domain.contract.repository.ContractVersionRepository;
import com.visafe.domain.member.entity.MemberDetails;
import com.visafe.domain.term.entity.ContractLaborTerms;
import com.visafe.domain.term.enums.TermField;
import com.visafe.domain.term.repository.ContractLaborTermsRepository;
import com.visafe.domain.term.repository.ContractTermAnnotationRepository;
import com.visafe.global.exception.CustomErrorCode;
import com.visafe.global.exception.RestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractRevisionService {

    private final ContractAccessService contractAccessService;
    private final ContractProcessingService contractProcessingService;
    private final ContractVersionRepository contractVersionRepository;
    private final ContractLaborTermsRepository contractLaborTermsRepository;
    private final ContractTermAnnotationRepository contractTermAnnotationRepository;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 돌아온 계약을 고칠 새 회차를 연다.
     *
     * <p>돌아온 계약 수정 시작 API(R8)에서 쓴다. stage가 RETURNED면 같은 원본 파일을 가리키는 새 회차(DRAFT)를 만들고
     * 이전 회차의 항목 값과 부가 정보를 복사한 뒤 현재 회차로 바꾸고 stage를 REVIEWING으로 둔다.
     * 이전 회차는 새 회차를 제출할 때 대체된다. 이미 REVIEWING이면 아무것도 바꾸지 않아 두 번 눌러도 안전하다.</p>
     *
     * @param contractId 계약 ID
     * @param member 로그인한 고용주
     * @return stage REVIEWING과 현재 회차 번호
     * @throws RestApiException 계약이 없거나 볼 권한이 없는 경우, 또는 stage가 RETURNED·REVIEWING이 아닌 경우
     */
    @Transactional
    public ContractStageResponse startRevision(Long contractId, MemberDetails member) {
        Contract contract = contractAccessService.getAccessibleContract(contractId, member);
        if (contract.getStage() == ContractStage.RETURNED) {
            openNextVersion(contract);
        } else if (contract.getStage() != ContractStage.REVIEWING) {
            throw new RestApiException(CustomErrorCode.CONTRACT_STAGE_CONFLICT);
        }
        return ContractStageResponse.of(contract, contractProcessingService.resolve(contract));
    }

    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * 이전 회차를 복사한 새 회차를 만들어 현재 회차로 바꾼다.
     *
     * @param contract 돌아온 계약
     */
    private void openNextVersion(Contract contract) {
        ContractVersion previous = contract.getCurrentVersion();
        ContractVersion next = contractVersionRepository.save(
                ContractVersion.create(contract, previous.getVersionNo() + 1, previous.getContractFile()));

        ContractLaborTerms previousTerms =
                contractLaborTermsRepository.findById(previous.getId()).orElseThrow();
        ContractLaborTerms terms = ContractLaborTerms.empty(next);
        for (TermField field : TermField.values()) {
            terms.changeValue(field, previousTerms.getValue(field));
        }
        contractLaborTermsRepository.save(terms);
        contractTermAnnotationRepository.saveAll(
                contractTermAnnotationRepository.findWithClauseByContractVersionId(previous.getId()).stream()
                        .map(annotation -> annotation.copyTo(next))
                        .toList());

        contract.changeCurrentVersion(next);
        contract.changeStage(ContractStage.REVIEWING);
    }
}
