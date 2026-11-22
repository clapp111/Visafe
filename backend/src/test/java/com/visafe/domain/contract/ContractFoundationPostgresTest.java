package com.visafe.domain.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.visafe.domain.company.entity.Company;
import com.visafe.domain.contract.dto.response.ProcessingResponse;
import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.entity.ContractFile;
import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.contract.enums.ContractEventType;
import com.visafe.domain.contract.enums.ProcessingTask;
import com.visafe.domain.contract.repository.ContractEventRepository;
import com.visafe.domain.contract.repository.ContractRepository;
import com.visafe.domain.contract.service.ContractEventRecorder;
import com.visafe.domain.contract.service.ContractProcessingService;
import com.visafe.domain.member.entity.Member;
import com.visafe.domain.member.repository.MemberRepository;
import com.visafe.domain.notification.entity.Notification;
import com.visafe.domain.notification.enums.NotificationType;
import com.visafe.domain.notification.service.NotificationPublisher;
import com.visafe.support.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import java.util.EnumSet;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** 계약 공통 부품을 실제 PostgreSQL 스키마에서 확인한다. */
@PostgresIntegrationTest
class ContractFoundationPostgresTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ContractEventRepository contractEventRepository;

    @Autowired
    private ContractEventRecorder eventRecorder;

    @Autowired
    private ContractProcessingService processingService;

    @Autowired
    private NotificationPublisher notificationPublisher;

    private Member employer;
    private Member worker1;
    private Member worker2;
    private Contract contract;

    @BeforeEach
    void setUp() {
        employer = member("employer@demo.visafe.test");
        worker1 = member("worker1@demo.visafe.test");
        worker2 = member("worker2@demo.visafe.test");

        contract = Contract.create("C-2026-99001", employer.getCompany(), employer, worker1, "vi", null, false);
        em.persist(contract);
        ContractFile file = ContractFile.builder()
                .contract(contract)
                .storageKey("local/contracts/it/a.pdf")
                .fileName("계약서.pdf")
                .mimeType("application/pdf")
                .sizeBytes(1024)
                .pageCount(2)
                .fileHash("0x" + "a".repeat(64))
                .uploadedBy(employer)
                .build();
        em.persist(file);
        ContractVersion version = ContractVersion.create(contract, 1, file);
        em.persist(version);
        contract.changeCurrentVersion(version);
        em.flush();
    }

    @Test
    void employerOfSameCompanyAndOwnWorkerCanSeeContract() {
        assertThat(contractRepository.findForEmployer(contract.getId(), employer.getId()))
                .contains(contract);
        assertThat(contractRepository.findForWorker(contract.getId(), worker1.getId()))
                .contains(contract);
    }

    @Test
    void otherCompanyEmployerAndOtherWorkerCannotSeeContract() {
        Company otherCompany = Company.create("다른사업장", "이영희", "000-00-00002", "서울특별시 중구 데모로 2");
        em.persist(otherCompany);
        Member otherEmployer = Member.employer("other@demo.visafe.test", "hash", "이영희", otherCompany);
        em.persist(otherEmployer);
        em.flush();

        assertThat(contractRepository.findForEmployer(contract.getId(), otherEmployer.getId()))
                .isEmpty();
        assertThat(contractRepository.findForWorker(contract.getId(), worker2.getId()))
                .isEmpty();
        // 근로자는 사업주가 없어 고용주 조회로는 찾을 수 없다.
        assertThat(contractRepository.findForEmployer(contract.getId(), worker1.getId()))
                .isEmpty();
    }

    @Test
    void recordedEventsDriveExtractionProcessing() {
        eventRecorder.record(contract, ContractEventType.FILE_UPLOADED, employer.getId(), Map.of("fileId", 1));
        eventRecorder.recordBySystem(contract, ContractEventType.EXTRACTION_STARTED, null);
        eventRecorder.recordBySystem(
                contract,
                ContractEventType.EXTRACTION_FAILED,
                Map.of(ContractProcessingService.ERROR_PAYLOAD_KEY, "OCR 실패"));
        em.flush();

        assertThat(contractEventRepository.findVersionEvents(
                        contract.getCurrentVersion().getId(), EnumSet.allOf(ContractEventType.class)))
                .extracting(event -> event.getEventType())
                .containsExactly(
                        ContractEventType.FILE_UPLOADED,
                        ContractEventType.EXTRACTION_STARTED,
                        ContractEventType.EXTRACTION_FAILED);
        assertThat(processingService.resolve(contract))
                .isEqualTo(ProcessingResponse.failed(ProcessingTask.EXTRACTION, 1, "OCR 실패"));
    }

    @Test
    void notificationPassesDatabaseConstraints() {
        Notification notification = notificationPublisher.publish(worker1, contract, NotificationType.REVIEW_REQUESTED);
        em.flush();

        assertThat(notification.getId()).isNotNull();
        assertThat(notification.getTitle()).isEqualTo("Có hợp đồng cần bạn xác nhận");
        assertThat(notification.getLinkPath()).isEqualTo("/contracts/" + contract.getId());
    }

    private Member member(String email) {
        return memberRepository.findByLoginEmail(email).orElseThrow();
    }
}
