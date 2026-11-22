package com.visafe.domain.contract.service;

import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.repository.ContractRepository;
import com.visafe.domain.member.entity.MemberDetails;
import com.visafe.global.exception.CustomErrorCode;
import com.visafe.global.exception.RestApiException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractAccessService {

    private final ContractRepository contractRepository;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 로그인한 회원이 볼 수 있는 계약을 조회한다.
     *
     * <p>고용주는 소속 사업주의 계약, 근로자는 본인 계약, 관리자는 모든 계약을 볼 수 있다.
     * 볼 권한이 없는 계약은 있는지도 알 수 없도록 없는 계약과 같은 {@code CONTRACT_NOT_FOUND}로 응답한다.
     * 역할별로 호출할 수 있는 API는 컨트롤러의 {@code @PreAuthorize}에서 따로 막는다.</p>
     *
     * @param contractId 조회할 계약 ID
     * @param member 로그인한 회원
     * @return 회원이 볼 수 있는 계약
     * @throws RestApiException 계약이 없거나 볼 권한이 없는 경우
     */
    public Contract getAccessibleContract(Long contractId, MemberDetails member) {
        return findAccessible(contractId, member)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.CONTRACT_NOT_FOUND));
    }

    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * 역할에 맞는 범위에서 계약을 찾는다.
     *
     * @param contractId 조회할 계약 ID
     * @param member 로그인한 회원
     * @return 볼 수 있는 계약, 없으면 빈 {@link Optional}
     */
    private Optional<Contract> findAccessible(Long contractId, MemberDetails member) {
        return switch (member.getRole()) {
            case ADMIN -> contractRepository.findById(contractId);
            case EMPLOYER -> contractRepository.findForEmployer(contractId, member.getMemberId());
            case WORKER -> contractRepository.findForWorker(contractId, member.getMemberId());
        };
    }
}
