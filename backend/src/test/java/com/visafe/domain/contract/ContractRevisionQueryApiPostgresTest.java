package com.visafe.domain.contract;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.visafe.domain.chain.ConfirmationNumber;
import com.visafe.domain.chain.entity.ChainRecord;
import com.visafe.domain.chain.recorder.ChainRecordRequestedEvent;
import com.visafe.domain.chain.recorder.ChainRecordWorker;
import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.contract.enums.ContractVersionStatus;
import com.visafe.domain.contract.extraction.ContractExtractionWorker;
import com.visafe.domain.contract.extraction.ExtractionRequestedEvent;
import com.visafe.domain.member.repository.MemberRepository;
import com.visafe.domain.term.entity.ContractLaborTerms;
import com.visafe.domain.term.entity.ContractTermAnnotation;
import com.visafe.domain.term.enums.TermField;
import com.visafe.domain.term.enums.ValueSource;
import com.visafe.domain.translation.entity.Translation;
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
 * 돌아온 계약 수정(R8), 검토 화면의 신고 내용(R4 returned), 내 계약 목록(M1), 계약 상세(M2)를 실제 PostgreSQL로 확인한다.
 * 계약은 API로 진행시키고 추출·번역·블록체인 기록은 stub 결과로 워커를 직접 호출해 실행한다. (S3는 메모리 가짜)
 */
@PostgresIntegrationTest
@AutoConfigureMockMvc
class ContractRevisionQueryApiPostgresTest {

    private static final String PASSWORD = "visafe-demo-1234";
    private static final String REASON = "Trong bản gốc ghi 30 phút.";

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

    @Autowired
    private ChainRecordWorker chainRecordWorker;

    @MockitoSpyBean
    private ContractTranslator translator;

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
    void returnedContractShowsReportAndRevisionCopiesVersionUntilResubmitted() throws Exception {
        Contract contract = returnedContract();

        getDraft(contract)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("RETURNED"))
                .andExpect(jsonPath("$.returned.by").value("WORKER"))
                .andExpect(jsonPath("$.returned.at").isNotEmpty())
                .andExpect(jsonPath("$.returned.reportedFields[0]").value("breakMinutes"))
                .andExpect(jsonPath("$.returned.reason").value(REASON))
                .andExpect(jsonPath("$.returned.reasonKo").value("[ko] " + REASON))
                .andExpect(jsonPath("$.attention[0].key").value("breakMinutes"))
                .andExpect(jsonPath("$.attention[0].reason").value("REPORTED"));

        startRevision(contract, employerToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("REVIEWING"))
                .andExpect(jsonPath("$.versionNo").value(2))
                .andExpect(jsonPath("$.processing").isEmpty());
        // 두 번 눌러도 새 회차를 또 만들지 않는다.
        startRevision(contract, employerToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionNo").value(2));
        em.flush();
        em.clear();

        List<ContractVersion> versions = versions(contract);
        assertThat(versions).extracting(ContractVersion::getVersionNo).containsExactly(1, 2);
        ContractVersion first = versions.get(0);
        ContractVersion second = versions.get(1);
        assertThat(first.getStatus()).isEqualTo(ContractVersionStatus.RETURNED);
        assertThat(second.getStatus()).isEqualTo(ContractVersionStatus.DRAFT);
        assertThat(second.getContractFile().getId())
                .isEqualTo(first.getContractFile().getId());
        assertThat(em.find(ContractLaborTerms.class, second.getId()).getValue(TermField.BREAK_MINUTES))
                .isEqualTo(60);
        assertThat(annotation(second, TermField.BREAK_MINUTES).getValueSource()).isEqualTo(ValueSource.EMPLOYER_INPUT);
        assertThat(annotation(second, TermField.MONTHLY_BASE_WAGE).getContractClause())
                .isNotNull();

        getDraft(contract)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("REVIEWING"))
                .andExpect(jsonPath("$.versionNo").value(2))
                .andExpect(jsonPath("$.returned.reportedFields[0]").value("breakMinutes"))
                .andExpect(jsonPath("$.fields[11].key").value("breakMinutes"))
                .andExpect(jsonPath("$.fields[11].valueSource").value("EMPLOYER_INPUT"));

        updateTerms(contract, "{\"breakMinutes\": 30}").andExpect(status().isOk());
        submit(contract)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.versionNo").value(2));
        em.flush();
        em.clear();

        assertThat(versions(contract))
                .extracting(ContractVersion::getStatus)
                .containsExactly(ContractVersionStatus.SUPERSEDED, ContractVersionStatus.SUBMITTED);
        assertThat(latestTranslation(contract).getTranslationNo()).isEqualTo(1);
    }

    @Test
    void revisionRules() throws Exception {
        Contract reviewing = extractedContract();
        startRevision(reviewing, employerToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionNo").value(1));
        assertThat(versions(reviewing)).hasSize(1);

        Contract translating = submittedContract();
        startRevision(translating, employerToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONTRACT_STAGE_CONFLICT"));
        startRevision(translating, workerToken).andExpect(status().isForbidden());
    }

    @Test
    void listShowsTodoFirstWithCountsForEachRole() throws Exception {
        Contract confirmed = confirmedContract();
        Contract awaiting = translatedContract();
        Contract uploaded = createContract();
        Contract reviewing = extractedContract();
        Contract returned = returnedContract();

        DocumentContext employerList = list(employerToken, "");
        assertThat(employerList.<Integer>read("$.counts.all")).isEqualTo(5);
        assertThat(employerList.<Integer>read("$.counts.inProgress")).isEqualTo(4);
        assertThat(employerList.<Integer>read("$.counts.confirmed")).isEqualTo(1);
        assertThat(employerList.<Integer>read("$.counts.terminated")).isZero();
        assertThat(employerList.<Integer>read("$.todoCount")).isEqualTo(2);
        assertThat(employerList.<List<Object>>read("$.content[0:2].contractId"))
                .containsExactlyInAnyOrder(id(returned), id(reviewing));
        assertThat(todoOf(employerList, returned)).isEqualTo("FIX_REQUIRED");
        assertThat(todoOf(employerList, reviewing)).isEqualTo("REVIEW_INCOMPLETE");
        assertThat(todoOf(employerList, awaiting)).isNull();
        assertThat(employerList.<List<String>>read(
                        "$.content[?(@.contractId == " + id(reviewing) + ")].counterpartyName"))
                .containsExactly("Nguyễn Văn An");
        assertThat(employerList.<List<String>>read("$.content[?(@.contractId == " + id(reviewing) + ")].startDate"))
                .containsExactly("2027-01-02");
        assertThat(employerList.<List<Object>>read("$.content[?(@.contractId == " + id(uploaded) + ")].startDate"))
                .containsExactly((Object) null);
        assertThat(employerList.<Integer>read("$.page.totalElements")).isEqualTo(5);

        DocumentContext confirmedOnly = list(employerToken, "status=CONFIRMED");
        assertThat(confirmedOnly.<List<Object>>read("$.content[*].contractId")).containsExactly(id(confirmed));
        assertThat(confirmedOnly.<Integer>read("$.counts.all")).isEqualTo(5);

        DocumentContext secondPage = list(employerToken, "page=1&size=2");
        assertThat(secondPage.<List<Object>>read("$.content")).hasSize(2);
        assertThat(secondPage.<Integer>read("$.page.totalPages")).isEqualTo(3);

        DocumentContext workerList = list(workerToken, "");
        assertThat(workerList.<Integer>read("$.todoCount")).isEqualTo(1);
        assertThat(workerList.<Integer>read("$.content[0].contractId")).isEqualTo(id(awaiting));
        assertThat(workerList.<String>read("$.content[0].todo")).isEqualTo("CONFIRM_REQUIRED");
        assertThat(workerList.<String>read("$.content[0].counterpartyName")).isEqualTo("한빛정밀");

        mockMvc.perform(get("/api/v1/contracts?size=0").header(HttpHeaders.AUTHORIZATION, employerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_INVALID_REQUEST"));
        mockMvc.perform(get("/api/v1/contracts?size=101").header(HttpHeaders.AUTHORIZATION, employerToken))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/contracts?status=DONE").header(HttpHeaders.AUTHORIZATION, employerToken))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/contracts").header(HttpHeaders.AUTHORIZATION, login("admin@demo.visafe.test")))
                .andExpect(status().isForbidden());
    }

    @Test
    void detailShowsTermsTimelineAndChainRecordForEachRole() throws Exception {
        Contract contract = confirmedContract();

        DocumentContext worker = detail(contract, workerToken);
        assertThat(worker.<String>read("$.status")).isEqualTo("CONFIRMED");
        assertThat(worker.<String>read("$.stage")).isEqualTo("DONE");
        assertThat(worker.<Object>read("$.processing")).isNull();
        assertThat(ConfirmationNumber.isValid(worker.read("$.confirmationNo"))).isTrue();
        assertThat(worker.<String>read("$.confirmedAt")).isNotNull();
        assertThat(worker.<String>read("$.employer.companyName")).isEqualTo("한빛정밀");
        assertThat(worker.<String>read("$.worker.nationality")).isEqualTo("VN");
        assertThat(worker.<String>read("$.file.mimeType")).isEqualTo("application/pdf");
        assertThat(worker.<List<Object>>read("$.terms")).hasSize(19);
        assertThat(worker.<String>read("$.terms[11].key")).isEqualTo("breakMinutes");
        assertThat(worker.<String>read("$.terms[11].valueSource")).isEqualTo("EMPLOYER_INPUT");
        assertThat(worker.<String>read("$.terms[11].explanation")).startsWith("[vi] ");
        assertThat(worker.<String>read("$.terms[9].value")).isEqualTo("09:00");

        // 이어서 고친 두 번의 항목 수정은 한 줄로 묶인다.
        assertThat(worker.<List<List<String>>>read("$.timeline[?(@.type == 'TERM_EDITED')].detail.keys"))
                .containsExactly(List.of("breakMinutes", "payDay"));
        assertThat(worker.<List<String>>read("$.timeline[?(@.type == 'SUBMITTED')].actorLabel"))
                .containsExactly("한빛정밀");
        assertThat(worker.<List<String>>read("$.timeline[?(@.type == 'WORKER_UNDERSTOOD')].actorLabel"))
                .containsExactly("Nguyễn Văn An");
        assertThat(worker.<List<String>>read("$.timeline[?(@.type == 'WORKER_UNDERSTOOD')].detail.language"))
                .containsExactly("vi");
        assertThat(worker.<List<Object>>read("$.timeline[?(@.type == 'CONFIRMED')].actorLabel"))
                .containsExactly((Object) null);
        assertThat(worker.<List<String>>read("$.timeline[*].type"))
                .containsSubsequence("CONTRACT_CREATED", "TERM_EDITED", "SUBMITTED", "WORKER_UNDERSTOOD", "CONFIRMED");

        assertThat(worker.<String>read("$.chainRecords[0].recordType")).isEqualTo("CONFIRM");
        assertThat(worker.<String>read("$.chainRecords[0].status")).isEqualTo("MINED");
        assertThat(worker.<String>read("$.chainRecords[0].network")).isEqualTo("stub");
        assertThat(worker.<String>read("$.chainRecords[0].explorerUrl")).isNull();
        assertThat(worker.<Object>read("$.termination")).isNull();
        assertThat(worker.<List<String>>read("$.availableActions"))
                .containsExactly("REQUEST_TERMINATION", "DOWNLOAD_FILE");

        DocumentContext employer = detail(contract, employerToken);
        assertThat(employer.<String>read("$.terms[11].explanation")).isEqualTo("휴게시간(분)은(는) 60입니다.");
        assertThat(employer.<List<String>>read("$.availableActions"))
                .containsExactly("REQUEST_TERMINATION", "DOWNLOAD_FILE");

        detailRequest(contract, login("admin@demo.visafe.test")).andExpect(status().isOk());
        detailRequest(contract, login("worker2@demo.visafe.test"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CONTRACT_NOT_FOUND"));
    }

    @Test
    void detailActionsAndListTodoFollowStage() throws Exception {
        Contract reviewing = extractedContract();
        assertThat(detail(reviewing, employerToken).<List<String>>read("$.availableActions"))
                .containsExactly("EDIT_DRAFT", "DOWNLOAD_FILE");

        Contract returned = returnedContract();
        DocumentContext returnedDetail = detail(returned, employerToken);
        assertThat(returnedDetail.<List<String>>read("$.availableActions"))
                .containsExactly("START_REVISION", "DOWNLOAD_FILE");
        assertThat(returnedDetail.<List<List<String>>>read("$.timeline[?(@.type == 'WORKER_REPORTED')].detail.keys"))
                .containsExactly(List.of("breakMinutes"));
        assertThat(returnedDetail.<List<String>>read("$.timeline[?(@.type == 'WORKER_REPORTED')].detail.reasonKo"))
                .containsExactly("[ko] " + REASON);

        Contract awaiting = translatedContract();
        assertThat(detail(awaiting, workerToken).<List<String>>read("$.availableActions"))
                .containsExactly("RESPOND_REVIEW", "DOWNLOAD_FILE");
        assertThat(detail(awaiting, employerToken).<List<String>>read("$.availableActions"))
                .containsExactly("DOWNLOAD_FILE");

        Contract failed = submittedContract();
        doThrow(new IllegalStateException("LLM 시간 초과")).when(translator).translate(any());
        translationWorker.translate(new TranslationRequestedEvent(
                failed.getId(), latestTranslation(failed).getId()));
        em.flush();
        em.clear();
        DocumentContext failedDetail = detail(failed, employerToken);
        assertThat(failedDetail.<String>read("$.processing.status")).isEqualTo("FAILED");
        assertThat(failedDetail.<List<String>>read("$.availableActions"))
                .containsExactly("RETRY_TRANSLATION", "DOWNLOAD_FILE");
        assertThat(todoOf(list(employerToken, ""), failed)).isEqualTo("TRANSLATION_FAILED");
    }

    /** 근로자가 "원본과 다릅니다"로 휴게시간을 신고해 돌아온 계약 */
    private Contract returnedContract() throws Exception {
        Contract contract = translatedContract();
        respond(
                        contract,
                        "{\"translationId\":" + latestTranslation(contract).getId() + ",\"action\":\"REPORTED\","
                                + "\"reportItems\":[{\"type\":\"ITEM_MISMATCH\",\"key\":\"breakMinutes\"}],"
                                + "\"reason\":\"" + REASON + "\"}")
                .andExpect(status().isAccepted());
        return reload(contract);
    }

    /** 근로자가 이해했다고 응답하고 블록체인 기록까지 끝난 계약 (항목 수정 두 번 포함) */
    private Contract confirmedContract() throws Exception {
        Contract contract = extractedContract();
        updateTerms(contract, "{\"breakMinutes\": 60}").andExpect(status().isOk());
        updateTerms(contract, "{\"payDay\": 25}").andExpect(status().isOk());
        submit(contract).andExpect(status().isAccepted());
        translate(contract);
        respond(
                        contract,
                        "{\"translationId\":" + latestTranslation(contract).getId()
                                + ",\"action\":\"UNDERSTOOD\",\"readAllChecked\":true}")
                .andExpect(status().isAccepted());
        em.flush();
        em.clear();
        ChainRecord record = em.createQuery(
                        "select r from ChainRecord r where r.contractVersion.contract.id = :id", ChainRecord.class)
                .setParameter("id", contract.getId())
                .getSingleResult();
        chainRecordWorker.record(new ChainRecordRequestedEvent(contract.getId(), record.getId()));
        return reload(contract);
    }

    private Contract translatedContract() throws Exception {
        Contract contract = submittedContract();
        translate(contract);
        return reload(contract);
    }

    /** 추출 후 비어 있던 필수 항목(휴게시간)을 채워 제출한 계약 */
    private Contract submittedContract() throws Exception {
        Contract contract = extractedContract();
        updateTerms(contract, "{\"breakMinutes\": 60}").andExpect(status().isOk());
        submit(contract).andExpect(status().isAccepted());
        return reload(contract);
    }

    private Contract extractedContract() throws Exception {
        Contract contract = createContract();
        ContractVersion version = contract.getCurrentVersion();
        extractionWorker.extract(new ExtractionRequestedEvent(
                contract.getId(), version.getId(), version.getContractFile().getId()));
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

    private void translate(Contract contract) {
        translationWorker.translate(new TranslationRequestedEvent(
                contract.getId(), latestTranslation(contract).getId()));
    }

    private Contract reload(Contract contract) {
        em.flush();
        em.clear();
        return em.find(Contract.class, contract.getId());
    }

    private ResultActions getDraft(Contract contract) throws Exception {
        return mockMvc.perform(get("/api/v1/contracts/" + contract.getId() + "/draft")
                .header(HttpHeaders.AUTHORIZATION, employerToken));
    }

    private ResultActions updateTerms(Contract contract, String values) throws Exception {
        return mockMvc.perform(patch("/api/v1/contracts/" + contract.getId() + "/draft/terms")
                .header(HttpHeaders.AUTHORIZATION, employerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"values\": " + values + "}"));
    }

    private ResultActions submit(Contract contract) throws Exception {
        return mockMvc.perform(post("/api/v1/contracts/" + contract.getId() + "/submit")
                .header(HttpHeaders.AUTHORIZATION, employerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"confirmedOriginalMatch\": true}"));
    }

    private ResultActions respond(Contract contract, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/contracts/" + contract.getId() + "/review-responses")
                .header(HttpHeaders.AUTHORIZATION, workerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions startRevision(Contract contract, String token) throws Exception {
        return mockMvc.perform(
                post("/api/v1/contracts/" + contract.getId() + "/revisions").header(HttpHeaders.AUTHORIZATION, token));
    }

    private DocumentContext list(String token, String query) throws Exception {
        String body = mockMvc.perform(get("/api/v1/contracts?" + query).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.parse(body);
    }

    private ResultActions detailRequest(Contract contract, String token) throws Exception {
        return mockMvc.perform(get("/api/v1/contracts/" + contract.getId()).header(HttpHeaders.AUTHORIZATION, token));
    }

    private DocumentContext detail(Contract contract, String token) throws Exception {
        String body = detailRequest(contract, token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contractNo").value(startsWith("C-")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.parse(body);
    }

    private static String todoOf(DocumentContext list, Contract contract) {
        List<String> todos = list.read("$.content[?(@.contractId == " + id(contract) + ")].todo");
        return todos.getFirst();
    }

    private static int id(Contract contract) {
        return contract.getId().intValue();
    }

    private List<ContractVersion> versions(Contract contract) {
        return em.createQuery(
                        "select v from ContractVersion v where v.contract.id = :id order by v.versionNo",
                        ContractVersion.class)
                .setParameter("id", contract.getId())
                .getResultList();
    }

    private ContractTermAnnotation annotation(ContractVersion version, TermField field) {
        return em.createQuery(
                        "select a from ContractTermAnnotation a where a.contractVersion.id = :id and a.fieldName = :field",
                        ContractTermAnnotation.class)
                .setParameter("id", version.getId())
                .setParameter("field", field)
                .getSingleResult();
    }

    private Translation latestTranslation(Contract contract) {
        return em.createQuery(
                        "select t from Translation t where t.contractVersion.contract.id = :id"
                                + " order by t.contractVersion.versionNo desc, t.translationNo desc",
                        Translation.class)
                .setParameter("id", contract.getId())
                .setMaxResults(1)
                .getSingleResult();
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
