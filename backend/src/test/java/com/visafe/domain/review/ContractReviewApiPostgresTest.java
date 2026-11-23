package com.visafe.domain.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.visafe.domain.chain.ChainRecordHash;
import com.visafe.domain.chain.ConfirmationNumber;
import com.visafe.domain.chain.EventSnapshot;
import com.visafe.domain.chain.entity.ChainRecord;
import com.visafe.domain.chain.enums.ChainRecordStatus;
import com.visafe.domain.chain.recorder.ChainRecordRequestedEvent;
import com.visafe.domain.chain.recorder.ChainRecordWorker;
import com.visafe.domain.chain.recorder.ChainRecorder;
import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.entity.ContractEvent;
import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.contract.enums.ContractEventType;
import com.visafe.domain.contract.enums.ContractStage;
import com.visafe.domain.contract.enums.ContractVersionStatus;
import com.visafe.domain.contract.extraction.ContractExtractionWorker;
import com.visafe.domain.contract.extraction.ExtractionRequestedEvent;
import com.visafe.domain.contract.repository.ContractEventRepository;
import com.visafe.domain.member.repository.MemberRepository;
import com.visafe.domain.notification.entity.Notification;
import com.visafe.domain.notification.enums.NotificationType;
import com.visafe.domain.review.entity.ReviewReportItem;
import com.visafe.domain.review.entity.ReviewResponse;
import com.visafe.domain.term.enums.TermField;
import com.visafe.domain.translation.entity.Translation;
import com.visafe.domain.translation.translator.ContractTranslationWorker;
import com.visafe.domain.translation.translator.TranslationRequestedEvent;
import com.visafe.global.storage.FileStorage;
import com.visafe.support.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
 * 확인할 계약 내용(W1), 근로자 응답(W2), 확정 기록을 실제 PostgreSQL로 확인한다.
 * 계약은 R1·R5·R6 API로 만들고 추출·번역·블록체인 기록은 stub 결과로 워커를 직접 호출해 실행한다.
 * (테스트가 롤백되므로 커밋 후 이벤트는 오지 않음, S3는 메모리 가짜)
 */
@PostgresIntegrationTest
@AutoConfigureMockMvc
class ContractReviewApiPostgresTest {

    private static final String PASSWORD = "visafe-demo-1234";
    private static final String REASON = "Trong bản gốc ghi 30 phút.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager em;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ContractEventRepository contractEventRepository;

    @Autowired
    private ContractExtractionWorker extractionWorker;

    @Autowired
    private ContractTranslationWorker translationWorker;

    @Autowired
    private ChainRecordWorker chainRecordWorker;

    @MockitoSpyBean
    private ChainRecorder chainRecorder;

    @MockitoBean
    private FileStorage fileStorage;

    private final Map<String, byte[]> stored = new HashMap<>();
    private String employerToken;
    private String workerToken;

    @BeforeEach
    void setUp() throws Exception {
        when(fileStorage.store(anyString(), any(), anyString())).thenAnswer(invocation -> {
            String key = "local/" + invocation.getArgument(0);
            stored.put(key, invocation.getArgument(1));
            return key;
        });
        when(fileStorage.read(anyString())).thenAnswer(invocation -> stored.get(invocation.<String>getArgument(0)));
        employerToken = login("employer@demo.visafe.test");
        workerToken = login("worker1@demo.visafe.test");
    }

    @Test
    void reviewShowsTranslatedItemsClausesAndWarnings() throws Exception {
        Contract contract = translatedContract();

        getReview(contract, workerToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.stage").value("AWAITING_WORKER"))
                .andExpect(jsonPath("$.canRespond").value(true))
                .andExpect(jsonPath("$.employer.companyName").value("한빛정밀"))
                .andExpect(jsonPath("$.employer.representativeName").value("김민수"))
                .andExpect(jsonPath("$.employer.memberName").value("김민수"))
                .andExpect(jsonPath("$.requestedAt").isNotEmpty())
                .andExpect(jsonPath("$.translationId")
                        .value(latestTranslation(contract).getId()))
                .andExpect(jsonPath("$.language").value("vi"))
                .andExpect(jsonPath("$.file.mimeType").value("application/pdf"))
                .andExpect(jsonPath("$.keyTerms.monthlyBaseWage").value(2_000_000))
                .andExpect(jsonPath("$.keyTerms.workStartTime").value("09:00"))
                .andExpect(jsonPath("$.keyTerms.weeklyHours").value(40.0))
                .andExpect(jsonPath("$.keyTerms.breakMinutes").value(60))
                .andExpect(jsonPath("$.keyTerms.startDate").value("2027-01-02"))
                // 2027년 최저임금 10,700원과 비교: 2,000,000 ÷ 209 = 9,569원
                .andExpect(jsonPath("$.warnings[0].code").value("MIN_WAGE"))
                .andExpect(jsonPath("$.warnings[0].key").value("monthlyBaseWage"))
                .andExpect(jsonPath("$.warnings[0].message").value(containsString("lương tối thiểu năm 2027")))
                .andExpect(jsonPath("$.warnings[0].messageKo").value(containsString("2027년 최저임금 시급 10,700원")))
                .andExpect(jsonPath("$.warnings[0].detail.hourlyWage").value(9_569))
                .andExpect(
                        jsonPath("$.warnings[?(@.code == 'NEEDS_CHECK')].key").value(hasItem("weeklyHoliday")))
                .andExpect(jsonPath("$.items.length()").value(19))
                .andExpect(item("breakMinutes", "value").value(60))
                .andExpect(item("breakMinutes", "translatedValue").value("[vi] 60"))
                .andExpect(item("breakMinutes", "easyExplanationKo").value(containsString("휴게시간")))
                .andExpect(item("breakMinutes", "valueSource").value("EMPLOYER_INPUT"))
                .andExpect(
                        item("breakMinutes", "employerInput.extractionStatus").value("UNREAD"))
                .andExpect(item("breakMinutes", "employerInput.aiValue").isEmpty())
                .andExpect(item("monthlyBaseWage", "valueSource").value("AI"))
                .andExpect(item("monthlyBaseWage", "employerInput").isEmpty())
                .andExpect(item("monthlyBaseWage", "sourceClause.clauseNo").value("제6조"))
                .andExpect(item("workStartTime", "value").value("09:00"))
                .andExpect(jsonPath("$.clauses.length()").value(10))
                .andExpect(jsonPath("$.clauses[1].seq").value(2))
                .andExpect(jsonPath("$.clauses[1].clauseNo").value("제1조"))
                .andExpect(jsonPath("$.clauses[1].translatedText").value(startsWith("[vi] ")));
    }

    @Test
    void reviewAccessRules() throws Exception {
        Contract submitted = submittedContract();
        getReview(submitted, workerToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("TRANSLATION_NOT_READY"));

        Contract translated = translatedContract();
        getReview(translated, employerToken).andExpect(status().isForbidden());
        getReview(translated, login("worker2@demo.visafe.test"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CONTRACT_NOT_FOUND"));

        respond(translated, understoodBody(translated)).andExpect(status().isAccepted());
        getReview(translated, workerToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canRespond").value(false));
    }

    @Test
    void understoodStartsRecordingThenConfirmsWhenMined() throws Exception {
        Contract contract = translatedContract();

        respond(contract, understoodBody(contract))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.stage").value("RECORDING"))
                .andExpect(jsonPath("$.processing.task").value("CHAIN_RECORD"))
                .andExpect(jsonPath("$.processing.status").value("RUNNING"))
                .andExpect(jsonPath("$.processing.attempt").value(1));
        em.flush();
        em.clear();

        Contract recording = em.find(Contract.class, contract.getId());
        ContractVersion version = recording.getCurrentVersion();
        assertThat(ConfirmationNumber.isValid(recording.getConfirmationNo())).isTrue();
        assertThat(reviewResponses(contract))
                .singleElement()
                .satisfies(response -> assertThat(response.isUnderstood()).isTrue());

        ChainRecord record = chainRecord(contract);
        assertThat(record.getStatus()).isEqualTo(ChainRecordStatus.PENDING);
        assertThat(record.getAnchorKey()).isEqualTo(ChainRecordHash.anchorKey(recording.getConfirmationNo()));
        assertThat(record.getFileHash()).isEqualTo(version.getContractFile().getFileHash());
        assertThat(record.getItemsHash()).isEqualTo(version.getItemsHash());
        assertThat(record.getTranslationHash())
                .isEqualTo(latestTranslation(contract).getTranslationHash());
        // DB에 남은 값만으로 이력 해시와 묶음 해시를 다시 계산할 수 있어야 한다.
        Long understoodEventId =
                events(contract, ContractEventType.WORKER_UNDERSTOOD).getFirst().getId();
        List<EventSnapshot> recordedEvents =
                contractEventRepository.findSnapshotsByContractId(contract.getId()).stream()
                        .filter(event -> event.id() <= understoodEventId)
                        .toList();
        assertThat(record.getEventsHash()).isEqualTo(ChainRecordHash.eventsHash(recordedEvents));
        assertThat(record.getRecordHash())
                .isEqualTo(ChainRecordHash.confirmRecordHash(
                        record.getAnchorKey(),
                        record.getEventsHash(),
                        record.getFileHash(),
                        record.getItemsHash(),
                        record.getTranslationHash(),
                        ChainRecordHash.workerId(recording.getWorker().getId(), record.getNonce())));
        assertThat(events(contract, ContractEventType.CHAIN_RECORD_REQUESTED)).hasSize(1);

        chainRecordWorker.record(new ChainRecordRequestedEvent(contract.getId(), record.getId()));
        em.flush();
        em.clear();

        Contract confirmed = em.find(Contract.class, contract.getId());
        assertThat(confirmed.getStage()).isEqualTo(ContractStage.DONE);
        assertThat(confirmed.getConfirmedAt()).isNotNull();
        assertThat(confirmed.getCurrentVersion().getStatus()).isEqualTo(ContractVersionStatus.CONFIRMED);
        ChainRecord mined = chainRecord(contract);
        assertThat(mined.getStatus()).isEqualTo(ChainRecordStatus.MINED);
        assertThat(mined.getTxHash()).matches("^0x[0-9a-f]{64}$");
        assertThat(events(contract, ContractEventType.CONFIRMED)).hasSize(1);
        assertThat(notifications(contract, NotificationType.CONTRACT_CONFIRMED))
                .extracting(notification -> notification.getMember().getLoginEmail())
                .containsExactlyInAnyOrder("employer@demo.visafe.test", "worker1@demo.visafe.test");
    }

    @Test
    void chainRecordFailureKeepsRecordingAndCountsRetry() throws Exception {
        Contract contract = translatedContract();
        respond(contract, understoodBody(contract)).andExpect(status().isAccepted());
        em.flush();
        em.clear();
        doThrow(new IllegalStateException("RPC 시간 초과")).when(chainRecorder).record(anyString(), any(), anyString());

        chainRecordWorker.record(new ChainRecordRequestedEvent(
                contract.getId(), chainRecord(contract).getId()));
        em.flush();
        em.clear();

        ChainRecord failed = chainRecord(contract);
        assertThat(failed.getStatus()).isEqualTo(ChainRecordStatus.FAILED);
        assertThat(failed.getRetryCount()).isEqualTo(1);
        assertThat(em.find(Contract.class, contract.getId()).getStage()).isEqualTo(ContractStage.RECORDING);
        assertThat(events(contract, ContractEventType.CHAIN_RECORD_FAILED)).hasSize(1);
    }

    @Test
    void understoodGoesToAuthorityReviewWhenRequired() throws Exception {
        Contract contract = translatedContract();
        em.createNativeQuery("update contracts set requires_authority_review = true where id = :id")
                .setParameter("id", contract.getId())
                .executeUpdate();
        em.clear();

        respond(contract, understoodBody(contract))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.stage").value("AUTHORITY_REVIEW"))
                .andExpect(jsonPath("$.processing").isEmpty());
        em.flush();
        em.clear();

        assertThat(em.find(Contract.class, contract.getId()).getConfirmationNo())
                .isNull();
        assertThat(events(contract, ContractEventType.AUTHORITY_REVIEW_REQUESTED))
                .hasSize(1);
        assertThat(chainRecords(contract)).isEmpty();
    }

    @Test
    void reportedReturnsContractToEmployerWithKoreanReason() throws Exception {
        Contract contract = translatedContract();
        String body = "{\"translationId\":" + latestTranslation(contract).getId() + ",\"action\":\"REPORTED\","
                + "\"reportItems\":[{\"type\":\"ITEM_MISMATCH\",\"key\":\"payDay\"},"
                + "{\"type\":\"ITEM_MISMATCH\",\"key\":\"payMethod\"},{\"type\":\"ITEM_MISMATCH\",\"key\":\"payDay\"}],"
                + "\"reason\":\"" + REASON + "\"}";

        respond(contract, body)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.stage").value("RETURNED"))
                .andExpect(jsonPath("$.processing").isEmpty());
        em.flush();
        em.clear();

        Contract returned = em.find(Contract.class, contract.getId());
        assertThat(returned.getCurrentVersion().getStatus()).isEqualTo(ContractVersionStatus.RETURNED);
        ReviewResponse response = reviewResponses(contract).getFirst();
        assertThat(response.isUnderstood()).isFalse();
        assertThat(response.getReason()).isEqualTo(REASON);
        assertThat(reportItems(response))
                .extracting(ReviewReportItem::getFieldName)
                .containsExactlyInAnyOrder(TermField.PAY_DAY, TermField.PAY_METHOD);
        assertThat(events(contract, ContractEventType.WORKER_REPORTED))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getPayload().get("keys")).isEqualTo(List.of("payDay", "payMethod"));
                    assertThat(event.getPayload().get("reasonKo")).isEqualTo("[ko] " + REASON);
                });
        assertThat(notifications(contract, NotificationType.CONTRACT_RETURNED))
                .singleElement()
                .satisfies(notification ->
                        assertThat(notification.getMember().getLoginEmail()).isEqualTo("employer@demo.visafe.test"));
    }

    @Test
    void responseRules() throws Exception {
        Contract contract = translatedContract();
        Long translationId = latestTranslation(contract).getId();

        respond(
                        contract,
                        "{\"translationId\":" + translationId + ",\"action\":\"UNDERSTOOD\",\"readAllChecked\":false}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("READ_ALL_REQUIRED"));
        respond(contract, "{\"translationId\":" + translationId + ",\"action\":\"REPORTED\",\"reportItems\":[]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("REPORT_ITEMS_REQUIRED"));
        respond(
                        contract,
                        "{\"translationId\":" + translationId
                                + ",\"action\":\"REPORTED\",\"reportItems\":[{\"type\":\"ITEM_MISMATCH\",\"key\":\"wage\"}]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("TERM_KEY_UNKNOWN"));
        respond(
                        contract,
                        "{\"translationId\":" + (translationId + 1000)
                                + ",\"action\":\"UNDERSTOOD\",\"readAllChecked\":true}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("TRANSLATION_OUTDATED"));
        mockMvc.perform(post("/api/v1/contracts/" + contract.getId() + "/review-responses")
                        .header(HttpHeaders.AUTHORIZATION, employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(understoodBody(contract)))
                .andExpect(status().isForbidden());
        assertThat(reviewResponses(contract)).isEmpty();

        respond(contract, understoodBody(contract)).andExpect(status().isAccepted());
        respond(contract, understoodBody(contract))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONTRACT_STAGE_CONFLICT"));
    }

    private Contract translatedContract() throws Exception {
        Contract contract = submittedContract();
        translationWorker.translate(new TranslationRequestedEvent(
                contract.getId(), latestTranslation(contract).getId()));
        em.flush();
        em.clear();
        return em.find(Contract.class, contract.getId());
    }

    /** 추출 후 비어 있던 필수 항목(휴게시간)을 채워 제출한 계약 */
    private Contract submittedContract() throws Exception {
        Contract contract = createContract();
        ContractVersion version = contract.getCurrentVersion();
        extractionWorker.extract(new ExtractionRequestedEvent(
                contract.getId(), version.getId(), version.getContractFile().getId()));
        em.flush();
        em.clear();
        mockMvc.perform(patch("/api/v1/contracts/" + contract.getId() + "/draft/terms")
                        .header(HttpHeaders.AUTHORIZATION, employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"values\": {\"breakMinutes\": 60}}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/contracts/" + contract.getId() + "/submit")
                        .header(HttpHeaders.AUTHORIZATION, employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmedOriginalMatch\": true}"))
                .andExpect(status().isAccepted());
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

    private ResultActions getReview(Contract contract, String token) throws Exception {
        return mockMvc.perform(
                get("/api/v1/contracts/" + contract.getId() + "/review").header(HttpHeaders.AUTHORIZATION, token));
    }

    private ResultActions respond(Contract contract, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/contracts/" + contract.getId() + "/review-responses")
                .header(HttpHeaders.AUTHORIZATION, workerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String understoodBody(Contract contract) {
        return "{\"translationId\":" + latestTranslation(contract).getId()
                + ",\"action\":\"UNDERSTOOD\",\"readAllChecked\":true}";
    }

    private static org.springframework.test.web.servlet.result.JsonPathResultMatchers item(String key, String path) {
        // 값이 모두 채워진 계약이라 항목 19개가 TermField 순서로 내려온다.
        int index =
                Arrays.asList(TermField.values()).indexOf(TermField.fromKey(key).orElseThrow());
        return jsonPath("$.items[" + index + "]." + path);
    }

    private Translation latestTranslation(Contract contract) {
        return em.createQuery(
                        "select t from Translation t where t.contractVersion.contract.id = :id order by t.translationNo desc",
                        Translation.class)
                .setParameter("id", contract.getId())
                .setMaxResults(1)
                .getSingleResult();
    }

    private ChainRecord chainRecord(Contract contract) {
        return chainRecords(contract).getFirst();
    }

    private List<ChainRecord> chainRecords(Contract contract) {
        return em.createQuery(
                        "select r from ChainRecord r where r.contractVersion.contract.id = :id", ChainRecord.class)
                .setParameter("id", contract.getId())
                .getResultList();
    }

    private List<ReviewResponse> reviewResponses(Contract contract) {
        return em.createQuery(
                        "select r from ReviewResponse r where r.translation.contractVersion.contract.id = :id",
                        ReviewResponse.class)
                .setParameter("id", contract.getId())
                .getResultList();
    }

    private List<ReviewReportItem> reportItems(ReviewResponse response) {
        return em.createQuery(
                        "select i from ReviewReportItem i where i.reviewResponse.id = :id", ReviewReportItem.class)
                .setParameter("id", response.getId())
                .getResultList();
    }

    private List<ContractEvent> events(Contract contract, ContractEventType type) {
        return em.createQuery(
                        "select e from ContractEvent e where e.contract.id = :id and e.eventType = :type order by e.id",
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
