package com.visafe.domain.contract;

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
import com.visafe.domain.chain.ChainRecordHash;
import com.visafe.domain.chain.EventSnapshot;
import com.visafe.domain.chain.entity.ChainRecord;
import com.visafe.domain.chain.enums.ChainRecordStatus;
import com.visafe.domain.chain.enums.RecordType;
import com.visafe.domain.chain.recorder.ChainRecordRequestedEvent;
import com.visafe.domain.chain.recorder.ChainRecordWorker;
import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.entity.ContractEvent;
import com.visafe.domain.contract.entity.ContractTermination;
import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.contract.enums.ContractEventType;
import com.visafe.domain.contract.enums.ContractStage;
import com.visafe.domain.contract.enums.TerminationReasonType;
import com.visafe.domain.contract.enums.TerminationStatus;
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
import java.time.ZoneId;
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
 * 합의 종료 요청·응답·취소(M4~M6), 종료일 처리(스케줄러), 새 계약으로의 대체 종료를 실제 PostgreSQL로 확인한다.
 * 확정 계약은 API로 진행시키고 추출·번역·블록체인 기록은 stub 결과로 워커를 직접 호출해 만든다.
 * 스케줄러는 날짜를 넘겨 직접 호출한다. (S3는 메모리 가짜)
 */
@PostgresIntegrationTest
@AutoConfigureMockMvc
class ContractTerminationApiPostgresTest {

    private static final String PASSWORD = "visafe-demo-1234";
    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Seoul"));
    /** stub 추출 결과의 계약 종료일 */
    private static final LocalDate CONTRACT_END = LocalDate.of(2029, 12, 31);

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
    void agreedTerminationEndsContractAfterEndDateAndRecordsOnChain() throws Exception {
        Contract contract = confirmedContract(null);
        LocalDate endDate = TODAY.plusDays(10);

        Long terminationId = idOf(requestTermination(contract, workerToken, endDate, "귀국 예정")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reasonType").value("MUTUAL"))
                .andExpect(jsonPath("$.reasonText").value("귀국 예정"))
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.endDate").value(endDate.toString())));
        assertThat(events(contract, ContractEventType.TERMINATION_REQUESTED)).hasSize(1);
        assertThat(recipients(contract, NotificationType.TERMINATION_REQUESTED))
                .containsExactly("employer@demo.visafe.test");

        DocumentContext employerDetail = detail(contract, employerToken);
        assertThat(employerDetail.<Integer>read("$.termination.terminationId")).isEqualTo(terminationId.intValue());
        assertThat(employerDetail.<Boolean>read("$.termination.requestedByMe")).isFalse();
        assertThat(employerDetail.<List<String>>read("$.availableActions"))
                .containsExactly("RESPOND_TERMINATION", "DOWNLOAD_FILE");
        DocumentContext workerDetail = detail(contract, workerToken);
        assertThat(workerDetail.<Boolean>read("$.termination.requestedByMe")).isTrue();
        assertThat(workerDetail.<List<String>>read("$.availableActions"))
                .containsExactly("CANCEL_TERMINATION", "DOWNLOAD_FILE");
        assertThat(todoOf(employerToken, contract)).isEqualTo("TERMINATION_RESPONSE");
        assertThat(todoOf(workerToken, contract)).isNull();

        requestTermination(contract, employerToken, endDate, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("TERMINATION_ALREADY_ACTIVE"));
        respond(contract, terminationId, workerToken, "AGREE")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("TERMINATION_NOT_ALLOWED"));
        respond(contract, terminationId, employerToken, "AGREE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AGREED"));
        respond(contract, terminationId, employerToken, "DECLINE")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("TERMINATION_STATUS_CONFLICT"));
        cancel(contract, terminationId, workerToken).andExpect(status().isConflict());
        assertThat(events(contract, ContractEventType.TERMINATION_AGREED)).hasSize(1);
        assertThat(recipients(contract, NotificationType.TERMINATION_RESPONDED))
                .containsExactly("worker1@demo.visafe.test");
        assertThat(detail(contract, workerToken).<List<String>>read("$.availableActions"))
                .containsExactly("DOWNLOAD_FILE");

        // 종료일은 마지막 근무일이라 그날에는 끝내지 않고 다음 날 끝낸다.
        scheduler.endDue(endDate);
        assertThat(reload(contract).getStage()).isEqualTo(ContractStage.DONE);
        scheduler.endDue(endDate.plusDays(1));
        Contract ended = reload(contract);
        assertThat(ended.getStage()).isEqualTo(ContractStage.ENDED);
        assertThat(ended.getTerminatedAt()).isNotNull();
        assertThat(events(contract, ContractEventType.TERMINATED)).hasSize(1);
        assertThat(recipients(contract, NotificationType.CONTRACT_TERMINATED))
                .containsExactlyInAnyOrder("employer@demo.visafe.test", "worker1@demo.visafe.test");

        ChainRecord confirmRecord = chainRecord(contract, RecordType.CONFIRM);
        ChainRecord terminateRecord = chainRecord(contract, RecordType.TERMINATE);
        assertThat(terminateRecord.getStatus()).isEqualTo(ChainRecordStatus.PENDING);
        assertThat(terminateRecord.getPreviousChainRecord().getId()).isEqualTo(confirmRecord.getId());
        assertThat(terminateRecord.getContractTermination().getId()).isEqualTo(terminationId);
        assertThat(terminateRecord.getAnchorKey()).isEqualTo(confirmRecord.getAnchorKey());
        // DB에 남은 값만으로 종료 기록의 묶음 해시를 다시 계산할 수 있어야 한다.
        Long terminatedEventId =
                events(contract, ContractEventType.TERMINATED).getFirst().getId();
        List<EventSnapshot> recordedEvents =
                contractEventRepository.findSnapshotsByContractId(contract.getId()).stream()
                        .filter(event -> event.id() <= terminatedEventId)
                        .toList();
        assertThat(terminateRecord.getEventsHash()).isEqualTo(ChainRecordHash.eventsHash(recordedEvents));
        assertThat(terminateRecord.getRecordHash())
                .isEqualTo(ChainRecordHash.terminateRecordHash(
                        terminateRecord.getAnchorKey(),
                        endDate,
                        terminateRecord.getEventsHash(),
                        confirmRecord.getRecordHash(),
                        TerminationReasonType.MUTUAL,
                        ChainRecordHash.workerId(ended.getWorker().getId(), terminateRecord.getNonce())));

        chainRecordWorker.record(new ChainRecordRequestedEvent(contract.getId(), terminateRecord.getId()));
        assertThat(chainRecord(contract, RecordType.TERMINATE).getStatus()).isEqualTo(ChainRecordStatus.MINED);
        assertThat(reload(contract).getStage()).isEqualTo(ContractStage.ENDED);

        // 종료 기록이 뒤에 붙어도 확정 기록의 무결성 검증 범위는 그대로다.
        mockMvc.perform(get("/api/v1/lookup/" + ended.getConfirmationNo()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TERMINATED"))
                .andExpect(jsonPath("$.termination.reasonType").value("MUTUAL"))
                .andExpect(jsonPath("$.termination.endDate").value(endDate.toString()))
                .andExpect(jsonPath("$.integrity.status").value("VERIFIED"))
                .andExpect(jsonPath("$.chainRecords.length()").value(2));
    }

    @Test
    void declinedAndCanceledRequestsLeaveContractConfirmed() throws Exception {
        Contract contract = confirmedContract(null);

        Long declined =
                idOf(requestTermination(contract, employerToken, TODAY, null).andExpect(status().isCreated()));
        assertThat(recipients(contract, NotificationType.TERMINATION_REQUESTED))
                .containsExactly("worker1@demo.visafe.test");
        respond(contract, declined, workerToken, "DECLINE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"));
        assertThat(recipients(contract, NotificationType.TERMINATION_RESPONDED))
                .containsExactly("employer@demo.visafe.test");
        assertThat(detail(contract, employerToken).<Object>read("$.termination"))
                .isNull();

        Long canceled = idOf(requestTermination(contract, employerToken, TODAY.plusDays(1), null)
                .andExpect(status().isCreated()));
        cancel(contract, canceled, workerToken)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("TERMINATION_NOT_ALLOWED"));
        cancel(contract, canceled, employerToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
        respond(contract, canceled, workerToken, "AGREE").andExpect(status().isConflict());

        assertThat(events(contract, ContractEventType.TERMINATION_DECLINED)).hasSize(1);
        assertThat(events(contract, ContractEventType.TERMINATION_CANCELED)).hasSize(1);
        scheduler.endDue(TODAY.plusDays(30));
        assertThat(reload(contract).getStage()).isEqualTo(ContractStage.DONE);
        assertThat(detail(contract, workerToken).<List<String>>read("$.availableActions"))
                .containsExactly("REQUEST_TERMINATION", "DOWNLOAD_FILE");
    }

    @Test
    void requestRules() throws Exception {
        Contract uploaded = createContract(null);
        requestTermination(uploaded, workerToken, TODAY, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONTRACT_STAGE_CONFLICT"));

        Contract contract = confirmedContract(null);
        requestTermination(contract, workerToken, TODAY.minusDays(1), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("TERMINATION_DATE_INVALID"));
        requestTermination(contract, workerToken, CONTRACT_END.plusDays(1), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("TERMINATION_DATE_INVALID"));
        requestTermination(contract, workerToken, CONTRACT_END, null).andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/contracts/" + contract.getId() + "/terminations")
                        .header(HttpHeaders.AUTHORIZATION, workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        requestTermination(contract, login("admin@demo.visafe.test"), TODAY, null)
                .andExpect(status().isForbidden());
        requestTermination(contract, login("worker2@demo.visafe.test"), TODAY, null)
                .andExpect(status().isNotFound());
        respond(contract, Long.MAX_VALUE, employerToken, "AGREE")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CONTRACT_NOT_FOUND"));
    }

    @Test
    void expiredContractEndsAndCancelsPendingRequest() throws Exception {
        Contract contract = confirmedContract(null);
        Long pending = idOf(requestTermination(contract, workerToken, TODAY.plusDays(5), null)
                .andExpect(status().isCreated()));

        scheduler.endDue(CONTRACT_END);
        assertThat(reload(contract).getStage()).isEqualTo(ContractStage.DONE);
        scheduler.endDue(CONTRACT_END.plusDays(1));

        assertThat(reload(contract).getStage()).isEqualTo(ContractStage.ENDED);
        assertThat(em.find(ContractTermination.class, pending).getStatus()).isEqualTo(TerminationStatus.CANCELED);
        ContractTermination expired =
                terminations(contract, TerminationReasonType.EXPIRED).getFirst();
        assertThat(expired.getStatus()).isEqualTo(TerminationStatus.AGREED);
        assertThat(expired.getEndDate()).isEqualTo(CONTRACT_END);
        assertThat(chainRecord(contract, RecordType.TERMINATE)
                        .getContractTermination()
                        .getId())
                .isEqualTo(expired.getId());
        assertThat(detail(contract, workerToken).<String>read("$.termination.reasonType"))
                .isEqualTo("EXPIRED");
    }

    @Test
    void confirmingReplacementEndsPreviousContract() throws Exception {
        Contract previous = confirmedContract(null);
        Contract replacement = recordingContract(previous.getId());

        // 대체되는 중인 계약은 종료를 요청할 수 없다.
        requestTermination(previous, workerToken, TODAY, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONTRACT_STAGE_CONFLICT"));
        assertThat(detail(previous, workerToken).<List<String>>read("$.availableActions"))
                .containsExactly("DOWNLOAD_FILE");

        chainRecordWorker.record(new ChainRecordRequestedEvent(
                replacement.getId(),
                chainRecord(replacement, RecordType.CONFIRM).getId()));

        assertThat(reload(replacement).getStage()).isEqualTo(ContractStage.DONE);
        Contract ended = reload(previous);
        assertThat(ended.getStage()).isEqualTo(ContractStage.ENDED);
        ContractTermination replaced =
                terminations(previous, TerminationReasonType.REPLACED).getFirst();
        assertThat(replaced.getStatus()).isEqualTo(TerminationStatus.AGREED);
        assertThat(replaced.getEndDate()).isEqualTo(TODAY);
        assertThat(replaced.getReasonText()).contains(replacement.getContractNo());
        assertThat(events(previous, ContractEventType.TERMINATED)).hasSize(1);
        assertThat(chainRecord(previous, RecordType.TERMINATE).getStatus()).isEqualTo(ChainRecordStatus.PENDING);
    }

    @Test
    void contractWithActiveTerminationCannotBeReplaced() throws Exception {
        Contract previous = confirmedContract(null);
        requestTermination(previous, workerToken, TODAY.plusDays(3), null).andExpect(status().isCreated());

        createContractRequest(previous.getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PREVIOUS_CONTRACT_INVALID"));
    }

    /** 근로자가 이해했다고 응답하고 블록체인 기록까지 끝난 계약 */
    private Contract confirmedContract(Long previousContractId) throws Exception {
        Contract contract = recordingContract(previousContractId);
        chainRecordWorker.record(new ChainRecordRequestedEvent(
                contract.getId(), chainRecord(contract, RecordType.CONFIRM).getId()));
        return reload(contract);
    }

    /** 근로자가 이해했다고 응답해 블록체인 기록을 시작한 계약 */
    private Contract recordingContract(Long previousContractId) throws Exception {
        Contract contract = createContract(previousContractId);
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
        reload(contract);
        mockMvc.perform(post("/api/v1/contracts/" + contract.getId() + "/review-responses")
                        .header(HttpHeaders.AUTHORIZATION, workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"translationId\":"
                                + latestTranslation(contract).getId()
                                + ",\"action\":\"UNDERSTOOD\",\"readAllChecked\":true}"))
                .andExpect(status().isAccepted());
        return reload(contract);
    }

    private Contract createContract(Long previousContractId) throws Exception {
        String body = createContractRequest(previousContractId)
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        em.flush();
        em.clear();
        return em.find(Contract.class, ((Number) JsonPath.read(body, "$.contractId")).longValue());
    }

    private ResultActions createContractRequest(Long previousContractId) throws Exception {
        Long workerId = memberRepository
                .findByLoginEmail("worker1@demo.visafe.test")
                .orElseThrow()
                .getId();
        String json = "{\"workerMemberId\":" + workerId + ",\"previousContractId\":" + previousContractId + "}";
        return mockMvc.perform(multipart("/api/v1/contracts")
                .file(new MockMultipartFile("file", "a.pdf", "application/pdf", pdf()))
                .file(new MockMultipartFile(
                        "data", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes(StandardCharsets.UTF_8)))
                .header(HttpHeaders.AUTHORIZATION, employerToken));
    }

    private ResultActions requestTermination(Contract contract, String token, LocalDate endDate, String reasonText)
            throws Exception {
        String reason = reasonText == null ? "null" : "\"" + reasonText + "\"";
        return mockMvc.perform(post("/api/v1/contracts/" + contract.getId() + "/terminations")
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"endDate\":\"" + endDate + "\",\"reasonText\":" + reason + "}"));
    }

    private ResultActions respond(Contract contract, Long terminationId, String token, String decision)
            throws Exception {
        return mockMvc.perform(
                post("/api/v1/contracts/" + contract.getId() + "/terminations/" + terminationId + "/response")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"" + decision + "\"}"));
    }

    private ResultActions cancel(Contract contract, Long terminationId, String token) throws Exception {
        return mockMvc.perform(
                post("/api/v1/contracts/" + contract.getId() + "/terminations/" + terminationId + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, token));
    }

    private DocumentContext detail(Contract contract, String token) throws Exception {
        String body = mockMvc.perform(
                        get("/api/v1/contracts/" + contract.getId()).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.parse(body);
    }

    private String todoOf(String token, Contract contract) throws Exception {
        String body = mockMvc.perform(get("/api/v1/contracts").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String> todos = JsonPath.read(body, "$.content[?(@.contractId == " + contract.getId() + ")].todo");
        return todos.getFirst();
    }

    private static Long idOf(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.terminationId")).longValue();
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

    private ChainRecord chainRecord(Contract contract, RecordType recordType) {
        em.flush();
        em.clear();
        return em.createQuery(
                        "select r from ChainRecord r where r.contractVersion.contract.id = :id and r.recordType = :type",
                        ChainRecord.class)
                .setParameter("id", contract.getId())
                .setParameter("type", recordType)
                .getSingleResult();
    }

    private List<ContractTermination> terminations(Contract contract, TerminationReasonType reasonType) {
        return em.createQuery(
                        "select t from ContractTermination t where t.contract.id = :id and t.reasonType = :type",
                        ContractTermination.class)
                .setParameter("id", contract.getId())
                .setParameter("type", reasonType)
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
