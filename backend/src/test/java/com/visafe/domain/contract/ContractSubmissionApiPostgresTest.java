package com.visafe.domain.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.entity.ContractEvent;
import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.contract.enums.ContractEventType;
import com.visafe.domain.contract.enums.ContractStage;
import com.visafe.domain.contract.enums.ContractVersionStatus;
import com.visafe.domain.contract.extraction.ContractExtractionWorker;
import com.visafe.domain.contract.extraction.ExtractionRequestedEvent;
import com.visafe.domain.member.repository.MemberRepository;
import com.visafe.domain.notification.entity.Notification;
import com.visafe.domain.notification.enums.NotificationType;
import com.visafe.domain.term.TermItemsHash;
import com.visafe.domain.term.entity.ContractLaborTerms;
import com.visafe.domain.translation.TranslationHash;
import com.visafe.domain.translation.entity.Translation;
import com.visafe.domain.translation.entity.TranslationClause;
import com.visafe.domain.translation.entity.TranslationItem;
import com.visafe.domain.translation.enums.TranslationStatus;
import com.visafe.domain.translation.translator.ContractTranslationWorker;
import com.visafe.domain.translation.translator.ContractTranslator;
import com.visafe.domain.translation.translator.TranslationRequestedEvent;
import com.visafe.global.storage.FileStorage;
import com.visafe.support.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 제출(R6)과 번역 다시 시도(R7), 비동기 번역 결과 저장을 실제 PostgreSQL로 확인한다.
 * 추출·번역은 stub 결과로 워커를 직접 호출해 실행한다. (테스트가 롤백되므로 커밋 후 이벤트는 오지 않음, S3는 메모리 가짜)
 */
@PostgresIntegrationTest
@AutoConfigureMockMvc
class ContractSubmissionApiPostgresTest {

    private static final String PASSWORD = "visafe-demo-1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager em;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ContractExtractionWorker extractionWorker;

    @Autowired
    private ContractTranslationWorker translationWorker;

    @MockitoSpyBean
    private ContractTranslator translator;

    @MockitoBean
    private FileStorage fileStorage;

    private final Map<String, byte[]> stored = new HashMap<>();
    private String employerToken;

    @BeforeEach
    void setUp() throws Exception {
        when(fileStorage.store(anyString(), any(), anyString())).thenAnswer(invocation -> {
            String key = "local/" + invocation.getArgument(0);
            stored.put(key, invocation.getArgument(1));
            return key;
        });
        when(fileStorage.read(anyString())).thenAnswer(invocation -> stored.get(invocation.<String>getArgument(0)));
        employerToken = login("employer@demo.visafe.test");
    }

    @Test
    void submitHashesTermsAndStartsTranslation() throws Exception {
        Contract contract = reviewedContract();

        submit(contract, true)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.stage").value("TRANSLATING"))
                .andExpect(jsonPath("$.versionNo").value(1))
                .andExpect(jsonPath("$.processing.task").value("TRANSLATION"))
                .andExpect(jsonPath("$.processing.status").value("RUNNING"))
                .andExpect(jsonPath("$.processing.attempt").value(1));
        em.flush();
        em.clear();

        ContractVersion version = em.find(Contract.class, contract.getId()).getCurrentVersion();
        ContractLaborTerms terms = em.find(ContractLaborTerms.class, version.getId());
        assertThat(version.getStatus()).isEqualTo(ContractVersionStatus.SUBMITTED);
        assertThat(version.getItemsHash()).isEqualTo(TermItemsHash.of(terms));
        assertThat(version.getSubmittedBy().getLoginEmail()).isEqualTo("employer@demo.visafe.test");
        assertThat(version.getSubmittedAt()).isNotNull();
        assertThat(events(contract, ContractEventType.SUBMITTED)).hasSize(1);
        assertThat(translations(contract)).singleElement().satisfies(translation -> {
            assertThat(translation.getLanguage()).isEqualTo("vi");
            assertThat(translation.getTranslationNo()).isEqualTo(1);
            assertThat(translation.getStatus()).isEqualTo(TranslationStatus.PENDING);
        });
    }

    @Test
    void translationSavesClausesAndItemsThenAwaitsWorker() throws Exception {
        Contract contract = submittedContract();

        translationWorker.translate(eventFor(contract));
        em.flush();
        em.clear();

        Contract translated = em.find(Contract.class, contract.getId());
        assertThat(translated.getStage()).isEqualTo(ContractStage.AWAITING_WORKER);
        Translation translation = translations(contract).getFirst();
        assertThat(translation.getStatus()).isEqualTo(TranslationStatus.DONE);
        assertThat(translation.getAttemptCount()).isEqualTo(1);
        assertThat(translation.getCompletedAt()).isNotNull();

        List<TranslationClause> clauses = translationClauses(translation);
        List<TranslationItem> items = translationItems(translation);
        // stub 추출 결과: 조항 10개, 항목은 고용주가 휴게시간을 채워 19개 모두 값이 있음
        assertThat(clauses).hasSize(10);
        assertThat(clauses)
                .allSatisfy(clause -> assertThat(clause.getTranslatedText()).startsWith("[vi] "));
        assertThat(items).hasSize(19);
        assertThat(translation.getTranslationHash()).isEqualTo(TranslationHash.of("vi", clauses, items));

        assertThat(events(contract, ContractEventType.TRANSLATION_COMPLETED)).hasSize(1);
        assertThat(notifications(contract, NotificationType.REVIEW_REQUESTED))
                .singleElement()
                .satisfies(notification ->
                        assertThat(notification.getMember().getLoginEmail()).isEqualTo("worker1@demo.visafe.test"));
    }

    @Test
    void translationFailsThreeTimesThenEmployerRetriesWithNewTranslation() throws Exception {
        Contract contract = submittedContract();
        String retryUrl = "/api/v1/contracts/" + contract.getId() + "/translations/retry";

        mockMvc.perform(post(retryUrl).header(HttpHeaders.AUTHORIZATION, employerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONTRACT_STAGE_CONFLICT"));

        doThrow(new IllegalStateException("LLM 시간 초과")).when(translator).translate(any());
        translationWorker.translate(eventFor(contract));
        em.flush();
        em.clear();

        Translation failed = translations(contract).getFirst();
        assertThat(failed.getStatus()).isEqualTo(TranslationStatus.FAILED);
        assertThat(failed.getAttemptCount()).isEqualTo(3);
        assertThat(em.find(Contract.class, contract.getId()).getStage()).isEqualTo(ContractStage.TRANSLATING);
        assertThat(events(contract, ContractEventType.TRANSLATION_FAILED)).hasSize(1);
        assertThat(notifications(contract, NotificationType.TRANSLATION_FAILED))
                .singleElement()
                .satisfies(notification ->
                        assertThat(notification.getMember().getLoginEmail()).isEqualTo("employer@demo.visafe.test"));

        mockMvc.perform(post(retryUrl).header(HttpHeaders.AUTHORIZATION, employerToken))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.stage").value("TRANSLATING"))
                .andExpect(jsonPath("$.processing.status").value("RUNNING"));
        em.flush();
        em.clear();

        List<Translation> translations = translations(contract);
        assertThat(translations).extracting(Translation::getTranslationNo).containsExactly(1, 2);
        Translation retried = translations.get(1);
        assertThat(retried.getStatus()).isEqualTo(TranslationStatus.PENDING);

        doCallRealMethod().when(translator).translate(any());
        translationWorker.translate(new TranslationRequestedEvent(contract.getId(), retried.getId()));
        em.flush();
        em.clear();

        assertThat(em.find(Translation.class, retried.getId()).getStatus()).isEqualTo(TranslationStatus.DONE);
        assertThat(em.find(Contract.class, contract.getId()).getStage()).isEqualTo(ContractStage.AWAITING_WORKER);
    }

    @Test
    void outdatedTranslationIsNotRun() throws Exception {
        Contract contract = submittedContract();
        TranslationRequestedEvent event = eventFor(contract);
        em.find(Contract.class, contract.getId()).changeStage(ContractStage.AWAITING_WORKER);
        em.flush();
        em.clear();

        translationWorker.translate(event);
        em.flush();
        em.clear();

        Translation translation = translations(contract).getFirst();
        assertThat(translation.getStatus()).isEqualTo(TranslationStatus.PENDING);
        assertThat(translation.getAttemptCount()).isZero();
    }

    @Test
    void submitRules() throws Exception {
        Contract uploaded = createContract();
        submit(uploaded, true)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONTRACT_STAGE_CONFLICT"));

        // stub 추출 결과는 휴게시간(필수)을 읽지 못한다.
        Contract extracted = extractedContract();
        submit(extracted, false)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("CONFIRMATION_REQUIRED"));
        mockMvc.perform(post("/api/v1/contracts/" + extracted.getId() + "/submit")
                        .header(HttpHeaders.AUTHORIZATION, employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("CONFIRMATION_REQUIRED"));
        submit(extracted, true)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("TERMS_REQUIRED_MISSING"))
                .andExpect(jsonPath("$.message").value(containsString("breakMinutes")));

        Contract submitted = submittedContract();
        submit(submitted, true).andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/contracts/" + submitted.getId() + "/submit")
                        .header(HttpHeaders.AUTHORIZATION, login("worker1@demo.visafe.test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmedOriginalMatch\": true}"))
                .andExpect(status().isForbidden());
    }

    private Contract submittedContract() throws Exception {
        Contract contract = reviewedContract();
        submit(contract, true).andExpect(status().isAccepted());
        em.flush();
        em.clear();
        return em.find(Contract.class, contract.getId());
    }

    /** 추출 후 비어 있던 필수 항목(휴게시간)을 채운 계약 */
    private Contract reviewedContract() throws Exception {
        Contract contract = extractedContract();
        mockMvc.perform(patch("/api/v1/contracts/" + contract.getId() + "/draft/terms")
                        .header(HttpHeaders.AUTHORIZATION, employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"values\": {\"breakMinutes\": 60}}"))
                .andExpect(status().isOk());
        em.flush();
        em.clear();
        return em.find(Contract.class, contract.getId());
    }

    private Contract extractedContract() throws Exception {
        Contract contract = createContract();
        ContractVersion version = contract.getCurrentVersion();
        extractionWorker.extract(new ExtractionRequestedEvent(
                contract.getId(), version.getId(), version.getContractFile().getId()));
        em.flush();
        em.clear();
        return em.find(Contract.class, contract.getId());
    }

    private Contract createContract() throws Exception {
        Long workerId = memberRepository
                .findByLoginEmail("worker1@demo.visafe.test")
                .orElseThrow()
                .getId();
        String json = "{\"workerMemberId\":" + workerId + ",\"previousContractId\":null}";
        String body = mockMvc.perform(multipart("/api/v1/contracts")
                        .file(new MockMultipartFile("file", "a.pdf", "application/pdf", pdf()))
                        .file(new MockMultipartFile(
                                "data", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes(StandardCharsets.UTF_8)))
                        .header(HttpHeaders.AUTHORIZATION, employerToken))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        em.flush();
        em.clear();
        return em.find(Contract.class, ((Number) JsonPath.read(body, "$.contractId")).longValue());
    }

    private ResultActions submit(Contract contract, boolean confirmed) throws Exception {
        return mockMvc.perform(post("/api/v1/contracts/" + contract.getId() + "/submit")
                .header(HttpHeaders.AUTHORIZATION, employerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"confirmedOriginalMatch\": " + confirmed + "}"));
    }

    private TranslationRequestedEvent eventFor(Contract contract) {
        return new TranslationRequestedEvent(
                contract.getId(), translations(contract).getLast().getId());
    }

    private List<Translation> translations(Contract contract) {
        return em.createQuery(
                        "select t from Translation t where t.contractVersion.contract.id = :id order by t.translationNo",
                        Translation.class)
                .setParameter("id", contract.getId())
                .getResultList();
    }

    private List<TranslationClause> translationClauses(Translation translation) {
        return em.createQuery(
                        "select c from TranslationClause c join fetch c.contractClause where c.translation.id = :id",
                        TranslationClause.class)
                .setParameter("id", translation.getId())
                .getResultList();
    }

    private List<TranslationItem> translationItems(Translation translation) {
        return em.createQuery("select i from TranslationItem i where i.translation.id = :id", TranslationItem.class)
                .setParameter("id", translation.getId())
                .getResultList();
    }

    private List<ContractEvent> events(Contract contract, ContractEventType type) {
        return em.createQuery(
                        "select e from ContractEvent e where e.contract.id = :id and e.eventType = :type",
                        ContractEvent.class)
                .setParameter("id", contract.getId())
                .setParameter("type", type)
                .getResultList();
    }

    private List<Notification> notifications(Contract contract, NotificationType type) {
        return em.createQuery(
                        "select n from Notification n join fetch n.member where n.contract.id = :id and n.type = :type",
                        Notification.class)
                .setParameter("id", contract.getId())
                .setParameter("type", type)
                .getResultList();
    }

    private static byte[] pdf() throws IOException {
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
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
