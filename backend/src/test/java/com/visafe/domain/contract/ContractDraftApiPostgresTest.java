package com.visafe.domain.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.visafe.domain.contract.extraction.ContractExtractionWorker;
import com.visafe.domain.contract.extraction.ExtractionRequestedEvent;
import com.visafe.domain.member.repository.MemberRepository;
import com.visafe.domain.term.entity.ContractTermEditLog;
import com.visafe.domain.term.enums.TermField;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 검토할 항목 조회(R4)와 항목 수정(R5)을 실제 PostgreSQL로 확인한다.
 * 계약은 R1로 만들고, 추출은 stub 추출기 결과로 워커를 직접 호출해 채운다. (S3는 메모리 가짜)
 */
@PostgresIntegrationTest
@AutoConfigureMockMvc
class ContractDraftApiPostgresTest {

    private static final String PASSWORD = "visafe-demo-1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager em;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ContractExtractionWorker extractionWorker;

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
    void draftBeforeExtractionHasNoFields() throws Exception {
        Contract contract = createContract();

        getDraft(contract)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("UPLOADED"))
                .andExpect(jsonPath("$.processing.task").value("EXTRACTION"))
                .andExpect(jsonPath("$.fields.length()").value(0))
                .andExpect(jsonPath("$.attention.length()").value(0));
    }

    @Test
    void draftShowsExtractedTermsWithWarningsAndSourceClauses() throws Exception {
        Contract contract = extractedContract();

        getDraft(contract)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("REVIEWING"))
                .andExpect(jsonPath("$.versionNo").value(1))
                .andExpect(jsonPath("$.processing").isEmpty())
                .andExpect(jsonPath("$.worker.name").value("Nguyễn Văn An"))
                .andExpect(jsonPath("$.worker.nationality").value("VN"))
                .andExpect(jsonPath("$.file.pageCount").value(1))
                .andExpect(jsonPath("$.returned").isEmpty())
                .andExpect(jsonPath("$.fields.length()").value(19))
                .andExpect(jsonPath("$.fields[0].key").value("employerName"))
                .andExpect(jsonPath("$.fields[18].key").value("accommodation"))
                // 시작일(2027년)의 최저임금 10,700원과 비교: 2,000,000 ÷ 209 = 9,569원
                .andExpect(field("monthlyBaseWage", "type").value("INTEGER"))
                .andExpect(field("monthlyBaseWage", "required").value(true))
                .andExpect(field("monthlyBaseWage", "value").value(2_000_000))
                .andExpect(field("monthlyBaseWage", "aiValue").value("2000000"))
                .andExpect(field("monthlyBaseWage", "valueSource").value("AI"))
                .andExpect(field("monthlyBaseWage", "warning.code").value("MIN_WAGE"))
                .andExpect(field("monthlyBaseWage", "warning.message").value(containsString("2027년 최저임금 시급 10,700원")))
                .andExpect(field("monthlyBaseWage", "warning.detail.hourlyWage").value(9_569))
                .andExpect(field("monthlyBaseWage", "sourceClause.clauseNo").value("제6조"))
                .andExpect(field("monthlyBaseWage", "sourceClause.pageNo").value(1))
                .andExpect(field("breakMinutes", "extractionStatus").value("UNREAD"))
                .andExpect(field("breakMinutes", "value").isEmpty())
                .andExpect(field("weeklyHoliday", "warning.code").value("NEEDS_CHECK"))
                .andExpect(field("workStartTime", "value").value("09:00"))
                .andExpect(field("startDate", "value").value("2027-01-02"))
                .andExpect(field("workerNationality", "required").value(false))
                .andExpect(jsonPath("$.attention[*].key").value(hasItem("monthlyBaseWage")))
                .andExpect(attention("monthlyBaseWage").value(hasItem("MIN_WAGE")))
                .andExpect(attention("breakMinutes").value(hasItem("UNREAD")))
                .andExpect(attention("weeklyHoliday").value(hasItem("NEEDS_CHECK")));
    }

    @Test
    void updateTermsSetsValueSourceLogsChangesAndRecalculatesWarnings() throws Exception {
        Contract contract = extractedContract();

        updateTerms(contract, "{\"breakMinutes\": 60, \"monthlyBaseWage\": 2300000, \"weeklyHoliday\": \"일요일\"}")
                .andExpect(status().isOk())
                .andExpect(field("breakMinutes", "value").value(60))
                .andExpect(field("breakMinutes", "valueSource").value("EMPLOYER_INPUT"))
                // 2,300,000 ÷ 209 = 11,004원 ≥ 10,700원
                .andExpect(field("monthlyBaseWage", "valueSource").value("EMPLOYER_EDIT"))
                .andExpect(field("monthlyBaseWage", "warning").isEmpty())
                // AI 값과 같은 값은 바뀐 것이 아니다
                .andExpect(field("weeklyHoliday", "valueSource").value("AI"))
                .andExpect(field("weeklyHoliday", "warning.code").value("NEEDS_CHECK"))
                .andExpect(jsonPath("$.attention[*].key").value(not(hasItem("breakMinutes"))))
                .andExpect(jsonPath("$.attention[*].key").value(not(hasItem("monthlyBaseWage"))));

        updateTerms(contract, "{\"monthlyBaseWage\": \"2000000\", \"weeklyHoliday\": \"토요일\"}")
                .andExpect(status().isOk())
                .andExpect(field("monthlyBaseWage", "valueSource").value("AI"))
                .andExpect(field("monthlyBaseWage", "warning.code").value("MIN_WAGE"))
                .andExpect(field("weeklyHoliday", "valueSource").value("EMPLOYER_EDIT"))
                .andExpect(field("weeklyHoliday", "warning").isEmpty());

        assertThat(editLogs(contract))
                .extracting(log -> log.getFieldName().getKey() + ":" + log.getBeforeValue() + "→" + log.getAfterValue())
                .containsExactlyInAnyOrder(
                        "breakMinutes:null→60",
                        "monthlyBaseWage:2000000→2300000",
                        "monthlyBaseWage:2300000→2000000",
                        "weeklyHoliday:일요일→토요일");
        assertThat(termEditedEvents(contract)).hasSize(2);
    }

    @Test
    void unchangedRequestLeavesNoHistory() throws Exception {
        Contract contract = extractedContract();

        updateTerms(contract, "{\"payDay\": 10}").andExpect(status().isOk());

        assertThat(editLogs(contract)).isEmpty();
        assertThat(termEditedEvents(contract)).isEmpty();
    }

    @Test
    void invalidValuesAreRejectedWithoutPartialChanges() throws Exception {
        Contract contract = extractedContract();

        updateTerms(contract, "{\"unknownKey\": 1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("TERM_KEY_UNKNOWN"));
        updateTerms(contract, "{\"breakMinutes\": 60, \"payDay\": 40}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("TERM_VALUE_INVALID"))
                .andExpect(jsonPath("$.message").value(startsWith("payDay:")));
        updateTerms(contract, "{\"endDate\": \"2026-12-31\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(startsWith("endDate:")));
        updateTerms(contract, "{\"workStartTime\": [9, 0]}").andExpect(status().isBadRequest());

        getDraft(contract).andExpect(field("breakMinutes", "value").isEmpty());
        assertThat(editLogs(contract)).isEmpty();
    }

    @Test
    void stageAndRoleRules() throws Exception {
        Contract uploaded = createContract();
        updateTerms(uploaded, "{\"breakMinutes\": 60}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONTRACT_STAGE_CONFLICT"));

        Contract submitted = extractedContract();
        submitted.changeStage(ContractStage.TRANSLATING);
        em.flush();
        getDraft(submitted).andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/contracts/" + submitted.getId() + "/draft")
                        .header(HttpHeaders.AUTHORIZATION, login("worker1@demo.visafe.test")))
                .andExpect(status().isForbidden());
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

    private static org.springframework.test.web.servlet.result.JsonPathResultMatchers field(String key, String path) {
        // 항목은 TermField 순서로 내려온다.
        return jsonPath("$.fields[" + TermField.fromKey(key).orElseThrow().ordinal() + "]." + path);
    }

    private static org.springframework.test.web.servlet.result.JsonPathResultMatchers attention(String key) {
        return jsonPath("$.attention[?(@.key == '" + key + "')].reason");
    }

    private List<ContractTermEditLog> editLogs(Contract contract) {
        return em.createQuery(
                        "select l from ContractTermEditLog l where l.contractVersion.contract.id = :id",
                        ContractTermEditLog.class)
                .setParameter("id", contract.getId())
                .getResultList();
    }

    private List<ContractEvent> termEditedEvents(Contract contract) {
        return em.createQuery(
                        "select e from ContractEvent e where e.contract.id = :id and e.eventType = :type",
                        ContractEvent.class)
                .setParameter("id", contract.getId())
                .setParameter("type", ContractEventType.TERM_EDITED)
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
