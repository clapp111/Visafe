package com.visafe.domain.authority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.visafe.domain.authority.entity.AuthorityReview;
import com.visafe.domain.authority.enums.ReviewDecision;
import com.visafe.domain.chain.ChainRecordHash;
import com.visafe.domain.chain.EventSnapshot;
import com.visafe.domain.chain.entity.ChainRecord;
import com.visafe.domain.chain.recorder.ChainRecordRequestedEvent;
import com.visafe.domain.chain.recorder.ChainRecordWorker;
import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.entity.ContractEvent;
import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.contract.enums.ContractEventType;
import com.visafe.domain.contract.enums.ContractStage;
import com.visafe.domain.contract.enums.ContractVersionStatus;
import com.visafe.domain.contract.extraction.ContractExtractionWorker;
import com.visafe.domain.contract.extraction.ExtractionRequestedEvent;
import com.visafe.domain.contract.repository.ContractEventRepository;
import com.visafe.domain.contract.scheduler.ContractTerminationScheduler;
import com.visafe.domain.member.repository.MemberRepository;
import com.visafe.domain.notification.entity.Notification;
import com.visafe.domain.notification.enums.NotificationType;
import com.visafe.domain.translation.entity.Translation;
import com.visafe.domain.translation.translator.ContractTranslationWorker;
import com.visafe.domain.translation.translator.TranslationRequestedEvent;
import com.visafe.global.storage.FileStorage;
import com.visafe.support.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 감독 기관 확인 대기 목록(D1), 계약 검토(D2), 승인(D3), 반려(D4)와 고용주 검토 화면의 반려 표시를 실제 PostgreSQL로 확인한다.
 * 기관 검토가 필요한 계약은 근로자 응답 전에 requires_authority_review를 켜서 만든다.
 * 추출·번역·블록체인 기록은 stub 결과로 워커를 직접 호출해 실행한다. (S3는 메모리 가짜)
 */
@PostgresIntegrationTest
@AutoConfigureMockMvc
class AuthorityApiPostgresTest {

    private static final String PASSWORD = "visafe-demo-1234";
    private static final String REASON = "월 기본급이 최저임금에 미달합니다. 원본과 함께 다시 확인해 주세요.";

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

    @Autowired
    private ContractTerminationScheduler scheduler;

    @MockitoBean
    private FileStorage fileStorage;

    private final Map<String, byte[]> stored = new HashMap<>();
    private String employerToken;
    private String workerToken;
    private String adminToken;

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
        adminToken = login("admin@demo.visafe.test");
    }

    @Test
    void pendingListAndReviewScreen() throws Exception {
        Contract contract = authorityReviewContract();

        DocumentContext pending = list("");
        assertThat(pending.<List<Integer>>read("$.content[*].contractId")).containsExactly(id(contract));
        assertThat(pending.<String>read("$.content[0].companyName")).isEqualTo("한빛정밀");
        assertThat(pending.<String>read("$.content[0].workerName")).isEqualTo("Nguyễn Văn An");
        assertThat(pending.<String>read("$.content[0].language")).isEqualTo("vi");
        assertThat(pending.<String>read("$.content[0].workerConfirmedAt")).isNotNull();
        // 최저임금 미달(2,000,000원) + AI 확인 필요(주휴일)
        assertThat(pending.<Integer>read("$.content[0].warningCount")).isEqualTo(2);
        // 고용주가 직접 입력한 휴게시간
        assertThat(pending.<Integer>read("$.content[0].employerInputCount")).isEqualTo(1);
        assertThat(pending.<Integer>read("$.page.totalElements")).isEqualTo(1);

        // 대소문자와 베트남어 성조를 무시하고 찾는다.
        assertThat(list("q=nguyen VAN").<List<Integer>>read("$.content[*].contractId"))
                .containsExactly(id(contract));
        assertThat(list("q=" + contract.getContractNo().substring(0, 6)).<List<Integer>>read("$.content[*].contractId"))
                .containsExactly(id(contract));
        assertThat(list("q=한빛").<List<Integer>>read("$.content[*].contractId")).containsExactly(id(contract));
        assertThat(list("q=없는이름").<List<Object>>read("$.content")).isEmpty();
        assertThat(list("view=APPROVED").<List<Object>>read("$.content")).isEmpty();

        DocumentContext review = detail(contract);
        assertThat(review.<String>read("$.stage")).isEqualTo("AUTHORITY_REVIEW");
        assertThat(review.<Integer>read("$.versionNo")).isEqualTo(1);
        assertThat(review.<String>read("$.employer.companyName")).isEqualTo("한빛정밀");
        assertThat(review.<String>read("$.worker.nationality")).isEqualTo("VN");
        assertThat(review.<String>read("$.file.mimeType")).isEqualTo("application/pdf");
        assertThat(review.<List<Object>>read("$.fields")).hasSize(19);
        assertThat(review.<String>read("$.fields[11].key")).isEqualTo("breakMinutes");
        assertThat(review.<String>read("$.fields[11].valueSource")).isEqualTo("EMPLOYER_INPUT");
        assertThat(review.<String>read("$.fields[15].warning.code")).isEqualTo("MIN_WAGE");
        assertThat(review.<String>read("$.editLogs[0].key")).isEqualTo("breakMinutes");
        assertThat(review.<Object>read("$.editLogs[0].before")).isNull();
        assertThat(review.<String>read("$.editLogs[0].after")).isEqualTo("60");
        assertThat(review.<List<String>>read("$.timeline[*].type"))
                .containsSubsequence("SUBMITTED", "WORKER_UNDERSTOOD", "AUTHORITY_REVIEW_REQUESTED");

        mockMvc.perform(get("/api/v1/admin/contracts").header(HttpHeaders.AUTHORIZATION, employerToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/contracts?view=DONE").header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/admin/contracts?size=0").header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_INVALID_REQUEST"));
    }

    @Test
    void approveStartsRecordingAndContractIsConfirmed() throws Exception {
        Contract contract = authorityReviewContract();

        approve(contract)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.stage").value("RECORDING"))
                .andExpect(jsonPath("$.processing.task").value("CHAIN_RECORD"));
        approve(contract)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONTRACT_STAGE_CONFLICT"));

        assertThat(review(contract).getDecision()).isEqualTo(ReviewDecision.APPROVED);
        assertThat(events(contract, ContractEventType.AUTHORITY_APPROVED)).hasSize(1);
        assertThat(recipients(contract, NotificationType.AUTHORITY_REVIEWED))
                .containsExactly("employer@demo.visafe.test");
        // 이력 해시에는 기관 승인까지 들어간다.
        ChainRecord record = confirmRecord(contract);
        Long approvedEventId = events(contract, ContractEventType.AUTHORITY_APPROVED)
                .getFirst()
                .getId();
        List<EventSnapshot> recordedEvents =
                contractEventRepository.findSnapshotsByContractId(contract.getId()).stream()
                        .filter(event -> event.id() <= approvedEventId)
                        .toList();
        assertThat(record.getEventsHash()).isEqualTo(ChainRecordHash.eventsHash(recordedEvents));

        chainRecordWorker.record(new ChainRecordRequestedEvent(contract.getId(), record.getId()));
        Contract confirmed = reload(contract);
        assertThat(confirmed.getStage()).isEqualTo(ContractStage.DONE);
        mockMvc.perform(get("/api/v1/lookup/" + confirmed.getConfirmationNo()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.integrity.status").value("VERIFIED"))
                .andExpect(jsonPath("$.history[*].type").value(org.hamcrest.Matchers.hasItem("AUTHORITY_APPROVED")));

        assertThat(list("view=APPROVED").<List<Integer>>read("$.content[*].contractId"))
                .containsExactly(id(contract));
        assertThat(list("").<List<Object>>read("$.content")).isEmpty();

        // 계약 종료일(2029-12-31)이 지나 끝나면 TERMINATED 보기에 나온다.
        scheduler.endDue(LocalDate.of(2030, 1, 1));
        assertThat(reload(contract).getStage()).isEqualTo(ContractStage.ENDED);
        assertThat(list("view=TERMINATED").<List<Integer>>read("$.content[*].contractId"))
                .containsExactly(id(contract));
    }

    @Test
    void rejectReturnsContractToEmployerWithReason() throws Exception {
        Contract contract = authorityReviewContract();

        reject(contract, "{\"reason\": \"  \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("REJECT_REASON_REQUIRED"));
        reject(contract, "{\"reason\": \"" + REASON + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("RETURNED"))
                .andExpect(jsonPath("$.processing").isEmpty());
        approve(contract).andExpect(status().isConflict());

        Contract returned = reload(contract);
        assertThat(returned.getCurrentVersion().getStatus()).isEqualTo(ContractVersionStatus.RETURNED);
        assertThat(review(contract).getReason()).isEqualTo(REASON);
        assertThat(events(contract, ContractEventType.AUTHORITY_REJECTED)).hasSize(1);
        assertThat(recipients(contract, NotificationType.AUTHORITY_REVIEWED))
                .containsExactly("employer@demo.visafe.test");
        assertThat(list("view=REJECTED").<List<Integer>>read("$.content[*].contractId"))
                .containsExactly(id(contract));

        mockMvc.perform(get("/api/v1/contracts/" + contract.getId() + "/draft")
                        .header(HttpHeaders.AUTHORIZATION, employerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returned.by").value("AUTHORITY"))
                .andExpect(jsonPath("$.returned.reason").value(REASON))
                .andExpect(jsonPath("$.returned.reasonKo").value(REASON))
                .andExpect(jsonPath("$.returned.reportedFields").isEmpty());
        DocumentContext employerDetail = JsonPath.parse(mockMvc.perform(
                        get("/api/v1/contracts/" + contract.getId()).header(HttpHeaders.AUTHORIZATION, employerToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        assertThat(employerDetail.<List<String>>read("$.timeline[?(@.type == 'AUTHORITY_REJECTED')].detail.reason"))
                .containsExactly(REASON);
        assertThat(employerDetail.<List<String>>read("$.availableActions"))
                .containsExactly("START_REVISION", "DOWNLOAD_FILE");

        // R8로 고치는 새 회차에서도 반려 사유를 계속 보여 준다.
        mockMvc.perform(post("/api/v1/contracts/" + contract.getId() + "/revisions")
                        .header(HttpHeaders.AUTHORIZATION, employerToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/contracts/" + contract.getId() + "/draft")
                        .header(HttpHeaders.AUTHORIZATION, employerToken))
                .andExpect(jsonPath("$.versionNo").value(2))
                .andExpect(jsonPath("$.returned.by").value("AUTHORITY"));
    }

    @Test
    void reviewRequiresAuthorityReviewStage() throws Exception {
        Contract awaiting = translatedContract();

        approve(awaiting)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONTRACT_STAGE_CONFLICT"));
        reject(awaiting, "{\"reason\": \"" + REASON + "\"}").andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/admin/contracts/" + awaiting.getId() + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, employerToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/contracts/" + Long.MAX_VALUE).header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CONTRACT_NOT_FOUND"));
    }

    /** 근로자가 이해했다고 응답해 기관 검토를 기다리는 계약 */
    private Contract authorityReviewContract() throws Exception {
        Contract contract = translatedContract();
        em.createNativeQuery("update contracts set requires_authority_review = true where id = :id")
                .setParameter("id", contract.getId())
                .executeUpdate();
        em.clear();
        mockMvc.perform(post("/api/v1/contracts/" + contract.getId() + "/review-responses")
                        .header(HttpHeaders.AUTHORIZATION, workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"translationId\":"
                                + latestTranslation(contract).getId()
                                + ",\"action\":\"UNDERSTOOD\",\"readAllChecked\":true}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.stage").value("AUTHORITY_REVIEW"));
        return reload(contract);
    }

    /** 휴게시간을 채워 제출하고 번역까지 끝나 근로자 확인을 기다리는 계약 */
    private Contract translatedContract() throws Exception {
        Contract contract = createContract();
        ContractVersion version = contract.getCurrentVersion();
        extractionWorker.extract(new ExtractionRequestedEvent(
                contract.getId(), version.getId(), version.getContractFile().getId()));
        reload(contract);
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
        reload(contract);
        translationWorker.translate(new TranslationRequestedEvent(
                contract.getId(), latestTranslation(contract).getId()));
        return reload(contract);
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

    private ResultActions approve(Contract contract) throws Exception {
        return mockMvc.perform(post("/api/v1/admin/contracts/" + contract.getId() + "/approve")
                .header(HttpHeaders.AUTHORIZATION, adminToken));
    }

    private ResultActions reject(Contract contract, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/admin/contracts/" + contract.getId() + "/reject")
                .header(HttpHeaders.AUTHORIZATION, adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private DocumentContext list(String query) throws Exception {
        String body = mockMvc.perform(
                        get("/api/v1/admin/contracts?" + query).header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.parse(body);
    }

    private DocumentContext detail(Contract contract) throws Exception {
        String body = mockMvc.perform(get("/api/v1/admin/contracts/" + contract.getId())
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.parse(body);
    }

    private static int id(Contract contract) {
        return contract.getId().intValue();
    }

    private Contract reload(Contract contract) {
        em.flush();
        em.clear();
        return em.find(Contract.class, contract.getId());
    }

    private Translation latestTranslation(Contract contract) {
        return em.createQuery(
                        "select t from Translation t where t.contractVersion.contract.id = :id order by t.translationNo desc",
                        Translation.class)
                .setParameter("id", contract.getId())
                .setMaxResults(1)
                .getSingleResult();
    }

    private AuthorityReview review(Contract contract) {
        em.flush();
        em.clear();
        return em.createQuery(
                        "select r from AuthorityReview r where r.contractVersion.contract.id = :id",
                        AuthorityReview.class)
                .setParameter("id", contract.getId())
                .getSingleResult();
    }

    private ChainRecord confirmRecord(Contract contract) {
        em.flush();
        em.clear();
        return em.createQuery(
                        "select r from ChainRecord r where r.contractVersion.contract.id = :id", ChainRecord.class)
                .setParameter("id", contract.getId())
                .getSingleResult();
    }

    private List<ContractEvent> events(Contract contract, ContractEventType type) {
        return em.createQuery(
                        "select e from ContractEvent e where e.contract.id = :id and e.eventType = :type order by e.id",
                        ContractEvent.class)
                .setParameter("id", contract.getId())
                .setParameter("type", type)
                .getResultList();
    }

    private List<String> recipients(Contract contract, NotificationType type) {
        return em
                .createQuery(
                        "select n from Notification n join fetch n.member where n.contract.id = :id and n.type = :type",
                        Notification.class)
                .setParameter("id", contract.getId())
                .setParameter("type", type)
                .getResultList()
                .stream()
                .map(notification -> notification.getMember().getLoginEmail())
                .toList();
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
