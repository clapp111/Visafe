package com.visafe.domain.contract.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.visafe.domain.contract.ContractFixtures;
import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.repository.ContractRepository;
import com.visafe.domain.member.entity.MemberDetails;
import com.visafe.domain.member.enums.MemberRole;
import com.visafe.global.exception.CustomErrorCode;
import com.visafe.global.exception.RestApiException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ContractAccessServiceTest {

    private final ContractRepository contractRepository = mock(ContractRepository.class);
    private final ContractAccessService service = new ContractAccessService(contractRepository);
    private final Contract contract = ContractFixtures.uploaded();

    @Test
    void employerLooksUpWithinOwnCompany() {
        when(contractRepository.findForEmployer(31L, 7L)).thenReturn(Optional.of(contract));

        assertThat(service.getAccessibleContract(31L, member(7L, MemberRole.EMPLOYER)))
                .isSameAs(contract);
        verify(contractRepository).findForEmployer(31L, 7L);
        verifyNoMoreInteractions(contractRepository);
    }

    @Test
    void workerLooksUpOwnContractOnly() {
        when(contractRepository.findForWorker(31L, 12L)).thenReturn(Optional.of(contract));

        assertThat(service.getAccessibleContract(31L, member(12L, MemberRole.WORKER)))
                .isSameAs(contract);
        verify(contractRepository).findForWorker(31L, 12L);
        verifyNoMoreInteractions(contractRepository);
    }

    @Test
    void adminSeesAnyContract() {
        when(contractRepository.findById(31L)).thenReturn(Optional.of(contract));

        assertThat(service.getAccessibleContract(31L, member(1L, MemberRole.ADMIN)))
                .isSameAs(contract);
    }

    @Test
    void contractOutsideScopeLooksLikeMissingContract() {
        when(contractRepository.findForWorker(31L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAccessibleContract(31L, member(99L, MemberRole.WORKER)))
                .isInstanceOfSatisfying(
                        RestApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(CustomErrorCode.CONTRACT_NOT_FOUND));
    }

    private static MemberDetails member(Long memberId, MemberRole role) {
        MemberDetails member = mock(MemberDetails.class);
        when(member.getMemberId()).thenReturn(memberId);
        when(member.getRole()).thenReturn(role);
        return member;
    }
}
