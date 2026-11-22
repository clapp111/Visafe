package com.visafe.domain.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.entity.ContractEvent;
import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.contract.enums.ContractEventType;
import com.visafe.domain.contract.enums.ContractStage;
import com.visafe.domain.contract.extraction.ContractExtractionWorker;
import com.visafe.domain.contract.extraction.ExtractionRequestedEvent;
import com.visafe.domain.contract.repository.ContractRepository;
import com.visafe.domain.member.repository.MemberRepository;
import com.visafe.domain.term.entity.ContractLaborTerms;
import com.visafe.domain.term.entity.ContractTermAnnotation;
import com.visafe.domain.term.enums.TermField;
import com.visafe.domain.term.enums.WarningCode;
import com.visafe.domain.term.repository.ContractLaborTermsRepository;
import com.visafe.global.crypto.Keccak256;
import com.visafe.global.storage.FileStorage;
import com.visafe.global.storage.PresignedDownload;
import com.visafe.support.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 계약 생성·원본 업로드·AI 추출(R1, R2, R3, M3)을 실제 PostgreSQL로 확인한다.
 * S3는 메모리 가짜로 바꾼다. 테스트 트랜잭션은 롤백되어 커밋 후 비동기 추출이 실행되지 않으므로, 추출은 워커를 직접 호출한다.
 */
@PostgresIntegrationTest
@AutoConfigureMockMvc
class ContractRegistrationApiPostgresTest {

    private static final String PASSWORD = "visafe-demo-1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager em;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ContractLaborTermsRepository contractLaborTermsRepository;

    @Autowired
    private ContractExtractionWorker extractionWorker;

    @MockitoBean
    private FileStorage fileStorage;

    private final Map<String, byte[]> stored = new HashMap<>();
    private String employerToken;
    private Long worker1Id;

    @BeforeEach
    void setUp() throws Exception {
        when(fileStorage.store(anyString(), org.mockito.ArgumentMatchers.any(), anyString()))
                .thenAnswer(invocation -> {
                    String key = "local/" + invocation.getArgument(0);
                    stored.put(key, invocation.getArgument(1));
                    return key;
                });
        when(fileStorage.read(anyString())).thenAnswer(invocation -> stored.get(invocation.<String>getArgument(0)));
        when(fileStorage.presignDownload(anyString(), anyString(), anyString(), anyBoolean()))
                .thenAnswer(invocation -> new PresignedDownload(
                        "https://s3.test/" + invocation.getArgument(0), Instant.parse("2026-11-13T05:11:00Z")));
        employerToken = login("employer@demo.visafe.test");
        worker1Id = memberRepository
                .findByLoginEmail("worker1@demo.visafe.test")
                .orElseThrow()
                .getId();
    }

    @Test
    void createStoresOriginalAndStartsExtraction() throws Exception {
        byte[] pdf = pdf(2);

        mockMvc.perform(createRequest(pdf, "표준근로계약서.pdf", worker1Id, null))
                .andExpect(status().isCreated())
                .andExpect(
                        header().string(HttpHeaders.LOCATION, org.hamcrest.Matchers.startsWith("/api/v1/contracts/")))
                .andExpect(jsonPath("$.contractNo").value(org.hamcrest.Matchers.matchesPattern("^C-\\d{4}-\\d{5}$")))
                .andExpect(jsonPath("$.stage").value("UPLOADED"))
                .andExpect(jsonPath("$.versionNo").value(1))
                .andExpect(jsonPath("$.processing.task").value("EXTRACTION"))
                .andExpect(jsonPath("$.processing.status").value("RUNNING"))
                .andExpect(jsonPath("$.processing.attempt").value(1));

        Contract contract = latestContract();
        ContractVersion version = contract.getCurrentVersion();
        assertThat(contract.getTargetLanguage()).isEqualTo("vi");
        assertThat(contract.isRequiresAuthorityReview()).isFalse();
        assertThat(version.getVersionNo()).isEqualTo(1);
        assertThat(version.isDraft()).isTrue();
        assertThat(version.getContractFile().getFileName()).isEqualTo("표준근로계약서.pdf");
        assertThat(version.getContractFile().getMimeType()).isEqualTo("application/pdf");
        assertThat(version.getContractFile().getPageCount()).isEqualTo(2);
        assertThat(version.getContractFile().getSizeBytes()).isEqualTo(pdf.length);
        assertThat(version.getContractFile().getFileHash()).isEqualTo(Keccak256.hash(pdf));
        assertThat(version.getContractFile().getStorageKey())
                .startsWith("local/contracts/" + contract.getId() + "/")
                .endsWith(".pdf");
        assertThat(eventTypes(contract))
                .containsExactly(
                        ContractEventType.CONTRACT_CREATED,
                        ContractEventType.FILE_UPLOADED,
                        ContractEventType.EXTRACTION_STARTED);
    }

    @Test
    void extractionSavesClausesAndTermsAndMovesToReviewing() throws Exception {
        mockMvc.perform(createRequest(pdf(1), "a.pdf", worker1Id, null)).andExpect(status().isCreated());
        Contract contract = latestContract();

        extractionWorker.extract(eventFor(contract));

        em.flush();
        em.clear();
        contract = contractRepository.findById(contract.getId()).orElseThrow();
        assertThat(contract.getStage()).isEqualTo(ContractStage.REVIEWING);
        ContractLaborTerms terms = contractLaborTermsRepository
                .findById(contract.getCurrentVersion().getId())
                .orElseThrow();
        assertThat(terms.getValue(TermField.MONTHLY_BASE_WAGE)).isEqualTo(2_000_000);
        assertThat(terms.getValue(TermField.BREAK_MINUTES)).isNull();
        Map<TermField, ContractTermAnnotation> annotations = annotations(contract.getCurrentVersion());
        assertThat(annotations).hasSize(TermField.values().length);
        assertThat(annotations.get(TermField.BREAK_MINUTES).isUnread()).isTrue();
        assertThat(annotations.get(TermField.WEEKLY_HOLIDAY).getWarningCode()).isEqualTo(WarningCode.NEEDS_CHECK);
        assertThat(annotations.get(TermField.MONTHLY_BASE_WAGE).getAiValue()).isEqualTo("2000000");
        assertThat(annotations
                        .get(TermField.MONTHLY_BASE_WAGE)
                        .getContractClause()
                        .getClauseNo())
                .isEqualTo("제6조");
        assertThat(eventTypes(contract)).endsWith(ContractEventType.EXTRACTION_COMPLETED);
    }

    @Test
    void resultOfReplacedOriginalIsDiscarded() throws Exception {
        mockMvc.perform(createRequest(pdf(1), "old.pdf", worker1Id, null)).andExpect(status().isCreated());
        Contract contract = latestContract();
        ExtractionRequestedEvent oldEvent = eventFor(contract);

        mockMvc.perform(replaceRequest(contract.getId(), pdf(3))).andExpect(status().isAccepted());
        ExtractionRequestedEvent newEvent =
                eventFor(contractRepository.findById(contract.getId()).orElseThrow());
        assertThat(newEvent.fileId()).isNotEqualTo(oldEvent.fileId());

        extractionWorker.extract(oldEvent);
        em.flush();
        em.clear();
        assertThat(contractRepository.findById(contract.getId()).orElseThrow().getStage())
                .isEqualTo(ContractStage.UPLOADED);
        assertThat(contractLaborTermsRepository.findById(newEvent.versionId())).isEmpty();

        extractionWorker.extract(newEvent);
        em.flush();
        em.clear();
        assertThat(contractRepository.findById(contract.getId()).orElseThrow().getStage())
                .isEqualTo(ContractStage.REVIEWING);
    }

    @Test
    void failedExtractionCanBeRetriedOnlyAfterFailure() throws Exception {
        mockMvc.perform(createRequest(pdf(1), "a.pdf", worker1Id, null)).andExpect(status().isCreated());
        Contract contract = latestContract();
        String retryUrl = "/api/v1/contracts/" + contract.getId() + "/extraction/retry";

        mockMvc.perform(post(retryUrl).header(HttpHeaders.AUTHORIZATION, employerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONTRACT_STAGE_CONFLICT"));

        when(fileStorage.read(anyString())).thenThrow(new IllegalStateException("S3 timeout"));
        extractionWorker.extract(eventFor(contract));

        mockMvc.perform(post(retryUrl).header(HttpHeaders.AUTHORIZATION, employerToken))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.stage").value("UPLOADED"))
                .andExpect(jsonPath("$.processing.status").value("RUNNING"))
                .andExpect(jsonPath("$.processing.attempt").value(2));
    }

    @Test
    void replaceFileFromReviewingClearsTermsAndRestartsExtraction() throws Exception {
        mockMvc.perform(createRequest(pdf(1), "a.pdf", worker1Id, null)).andExpect(status().isCreated());
        Contract contract = latestContract();
        extractionWorker.extract(eventFor(contract));
        em.flush();
        em.clear();

        mockMvc.perform(replaceRequest(contract.getId(), pdf(2)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.stage").value("UPLOADED"))
                .andExpect(jsonPath("$.processing.task").value("EXTRACTION"));

        em.flush();
        em.clear();
        Contract reloaded = contractRepository.findById(contract.getId()).orElseThrow();
        assertThat(reloaded.getCurrentVersion().getContractFile().getPageCount())
                .isEqualTo(2);
        assertThat(contractLaborTermsRepository.findById(
                        reloaded.getCurrentVersion().getId()))
                .isEmpty();
        assertThat(annotations(reloaded.getCurrentVersion())).isEmpty();
    }

    @Test
    void replaceFileAfterSubmitIsRejected() throws Exception {
        mockMvc.perform(createRequest(pdf(1), "a.pdf", worker1Id, null)).andExpect(status().isCreated());
        Contract contract = latestContract();
        contract.changeStage(ContractStage.REVIEWING);
        contract.changeStage(ContractStage.TRANSLATING);
        em.flush();

        mockMvc.perform(replaceRequest(contract.getId(), pdf(1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONTRACT_STAGE_CONFLICT"));
    }

    @Test
    void createRejectsInvalidRequests() throws Exception {
        Long employerId = memberRepository
                .findByLoginEmail("employer@demo.visafe.test")
                .orElseThrow()
                .getId();
        mockMvc.perform(createRequest(pdf(1), "a.pdf", employerId, null))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("WORKER_NOT_FOUND"));
        mockMvc.perform(createRequest("hello".getBytes(StandardCharsets.UTF_8), "a.pdf", worker1Id, null))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.errorCode").value("FILE_UNSUPPORTED_TYPE"));
        mockMvc.perform(createRequest("%PDF-broken".getBytes(StandardCharsets.US_ASCII), "a.pdf", worker1Id, null))
                .andExpect(status().isUnsupportedMediaType());

        mockMvc.perform(createRequest(pdf(1), "a.pdf", worker1Id, null)).andExpect(status().isCreated());
        Long notConfirmedId = latestContract().getId();
        mockMvc.perform(createRequest(pdf(1), "b.pdf", worker1Id, notConfirmedId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PREVIOUS_CONTRACT_INVALID"));

        mockMvc.perform(multipart("/api/v1/contracts")
                        .file(new MockMultipartFile("file", "a.pdf", "application/pdf", pdf(1)))
                        .header(HttpHeaders.AUTHORIZATION, employerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_INVALID_PARAMETER"));
    }

    @Test
    void onlyEmployerCanCreate() throws Exception {
        mockMvc.perform(multipart("/api/v1/contracts")
                        .file(new MockMultipartFile("file", "a.pdf", "application/pdf", pdf(1)))
                        .file(data(worker1Id, null))
                        .header(HttpHeaders.AUTHORIZATION, login("worker1@demo.visafe.test")))
                .andExpect(status().isForbidden());
    }

    @Test
    void downloadUrlIsGivenToPartiesForTheirOwnFileOnly() throws Exception {
        mockMvc.perform(createRequest(pdf(1), "원본.pdf", worker1Id, null)).andExpect(status().isCreated());
        Contract contract = latestContract();
        Long fileId = contract.getCurrentVersion().getContractFile().getId();
        String url = "/api/v1/contracts/" + contract.getId() + "/files/" + fileId + "/download-url";

        mockMvc.perform(get(url).param("inline", "true").header(HttpHeaders.AUTHORIZATION, employerToken))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.url").value(org.hamcrest.Matchers.startsWith("https://s3.test/local/contracts/")))
                .andExpect(jsonPath("$.fileName").value("원본.pdf"))
                .andExpect(jsonPath("$.mimeType").value("application/pdf"));
        org.mockito.Mockito.verify(fileStorage)
                .presignDownload(anyString(), eq("원본.pdf"), eq("application/pdf"), eq(true));

        mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, login("worker1@demo.visafe.test")))
                .andExpect(status().isOk());
        mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, login("worker2@demo.visafe.test")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CONTRACT_NOT_FOUND"));

        mockMvc.perform(createRequest(pdf(1), "other.pdf", worker1Id, null)).andExpect(status().isCreated());
        Long otherContractId = latestContract().getId();
        mockMvc.perform(get("/api/v1/contracts/" + otherContractId + "/files/" + fileId + "/download-url")
                        .header(HttpHeaders.AUTHORIZATION, employerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("FILE_NOT_FOUND"));
    }

    private org.springframework.test.web.servlet.RequestBuilder createRequest(
            byte[] content, String fileName, Long workerMemberId, Long previousContractId) {
        return multipart("/api/v1/contracts")
                .file(new MockMultipartFile("file", fileName, "application/pdf", content))
                .file(data(workerMemberId, previousContractId))
                .header(HttpHeaders.AUTHORIZATION, employerToken);
    }

    private org.springframework.test.web.servlet.RequestBuilder replaceRequest(Long contractId, byte[] content) {
        return multipart(HttpMethod.PUT, "/api/v1/contracts/" + contractId + "/file")
                .file(new MockMultipartFile("file", "new.pdf", "application/pdf", content))
                .header(HttpHeaders.AUTHORIZATION, employerToken);
    }

    private static MockMultipartFile data(Long workerMemberId, Long previousContractId) {
        String json = "{\"workerMemberId\":" + workerMemberId + ",\"previousContractId\":" + previousContractId + "}";
        return new MockMultipartFile(
                "data", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes(StandardCharsets.UTF_8));
    }

    private Contract latestContract() {
        em.flush();
        em.clear();
        return em.createQuery("select c from Contract c order by c.id desc", Contract.class)
                .setMaxResults(1)
                .getSingleResult();
    }

    private static ExtractionRequestedEvent eventFor(Contract contract) {
        ContractVersion version = contract.getCurrentVersion();
        return new ExtractionRequestedEvent(
                contract.getId(), version.getId(), version.getContractFile().getId());
    }

    private List<ContractEventType> eventTypes(Contract contract) {
        return em
                .createQuery(
                        "select e from ContractEvent e where e.contract.id = :id order by e.createdAt, e.id",
                        ContractEvent.class)
                .setParameter("id", contract.getId())
                .getResultList()
                .stream()
                .map(ContractEvent::getEventType)
                .toList();
    }

    private Map<TermField, ContractTermAnnotation> annotations(ContractVersion version) {
        Map<TermField, ContractTermAnnotation> result = new HashMap<>();
        em.createQuery(
                        "select a from ContractTermAnnotation a where a.contractVersion.id = :id",
                        ContractTermAnnotation.class)
                .setParameter("id", version.getId())
                .getResultList()
                .forEach(annotation -> result.put(annotation.getFieldName(), annotation));
        return result;
    }

    private static byte[] pdf(int pages) throws IOException {
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int i = 0; i < pages; i++) {
                document.addPage(new PDPage());
            }
            document.save(out);
            return out.toByteArray();
        }
    }

    private String login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.accessToken");
    }
}
