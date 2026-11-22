package com.visafe.domain.lookup;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.visafe.domain.chain.recorder.ChainRecorder;
import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.entity.ContractTermination;
import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.contract.extraction.ContractExtractionWorker;
import com.visafe.domain.contract.extraction.ExtractionRequestedEvent;
import com.visafe.domain.member.repository.MemberRepository;
import com.visafe.domain.translation.entity.Translation;
import com.visafe.domain.translation.translator.ContractTranslationWorker;
import com.visafe.domain.translation.translator.TranslationRequestedEvent;
import com.visafe.global.storage.FileStorage;
import com.visafe.support.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
 * 확인번호 조회(L1)와 파일 대조(L2)를 실제 PostgreSQL로 확인한다. 로그인 없이 호출한다.
 * 확정 계약은 API로 진행시키고 추출·번역·블록체인 기록은 stub 결과로 워커를 직접 호출해 만든다. (S3는 메모리 가짜)
 */
@PostgresIntegrationTest
@AutoConfigureMockMvc
class LookupApiPostgresTest {

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
    void lookupShowsPublicInfoWithoutWageAndVerifiesIntegrity() throws Exception {
        Contract contract = confirmedContract();
        // 소문자·하이픈 없이 입력해도 찾는다.
        String typed = contract.getConfirmationNo().replace("-", "").toLowerCase(Locale.ROOT);

        DocumentContext body = JsonPath.parse(lookup(typed)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyBaseWage").doesNotExist())
                .andExpect(jsonPath("$.keyTerms.monthlyBaseWage").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString());

        assertThat(body.<String>read("$.confirmationNo")).isEqualTo(contract.getConfirmationNo());
        assertThat(body.<String>read("$.status")).isEqualTo("CONFIRMED");
        assertThat(body.<String>read("$.companyName")).isEqualTo("한빛정밀");
        assertThat(body.<String>read("$.workerNameMasked")).isEqualTo("N***** V** An");
        assertThat(body.<String>read("$.startDate")).isEqualTo("2027-01-02");
        assertThat(body.<String>read("$.endDate")).isEqualTo("2029-12-31");
        assertThat(body.<String>read("$.confirmedAt")).isNotNull();
        assertThat(body.<String>read("$.keyTerms.workStartTime")).isEqualTo("09:00");
        assertThat(body.<String>read("$.keyTerms.workEndTime")).isEqualTo("18:00");
        assertThat(body.<Integer>read("$.keyTerms.breakMinutes")).isEqualTo(60);
        assertThat(body.<Integer>read("$.keyTerms.workDaysPerWeek")).isEqualTo(5);
        assertThat(body.<Double>read("$.keyTerms.weeklyHours")).isEqualTo(40.0);
        assertThat(body.<String>read("$.integrity.status")).isEqualTo("VERIFIED");
        assertThat(body.<String>read("$.integrity.checkedAt")).isNotNull();
        assertThat(body.<Object>read("$.termination")).isNull();
        assertThat(body.<String>read("$.chainRecords[0].status")).isEqualTo("MINED");
        assertThat(body.<String>read("$.chainRecords[0].explorerUrl")).startsWith("https://sepolia.etherscan.io/tx/");
        // 항목 수정 같은 내부 이벤트는 넣지 않고, 근로자 이름은 이력에도 드러내지 않는다.
        assertThat(body.<List<String>>read("$.history[*].type"))
                .containsExactly("SUBMITTED", "WORKER_UNDERSTOOD", "CONFIRMED");
        assertThat(body.<String>read("$.history[0].actorLabel")).isEqualTo("한빛정밀");
        assertThat(body.<Object>read("$.history[1].actorLabel")).isNull();
        assertThat(body.<String>read("$.history[1].detail.language")).isEqualTo("vi");
    }

    @Test
    void changedTermsAfterConfirmationAreDetected() throws Exception {
        Contract contract = confirmedContract();
        em.createNativeQuery("update contract_labor_terms set break_minutes = 30 where contract_version_id = :id")
                .setParameter("id", contract.getCurrentVersion().getId())
                .executeUpdate();
        em.clear();

        lookup(contract.getConfirmationNo())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.integrity.status").value("MISMATCH"));
    }

    @Test
    void chainReadFailureIsUnavailable() throws Exception {
        Contract contract = confirmedContract();
        doThrow(new IllegalStateException("RPC 시간 초과")).when(chainRecorder).readRecordHash(anyString());

        lookup(contract.getConfirmationNo())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.integrity.status").value("UNAVAILABLE"));
    }

    @Test
    void terminatedContractShowsTermination() throws Exception {
        Contract contract = confirmedContract();
        Instant terminatedAt = Instant.parse("2027-06-30T15:00:00Z");
        Contract managed = em.find(Contract.class, contract.getId());
        managed.terminate(terminatedAt);
        em.persist(ContractTermination.expired(managed, LocalDate.of(2027, 6, 30)));
        em.flush();
        em.clear();

        lookup(contract.getConfirmationNo())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TERMINATED"))
                .andExpect(jsonPath("$.termination.reasonType").value("EXPIRED"))
                .andExpect(jsonPath("$.termination.endDate").value("2027-06-30"))
                .andExpect(jsonPath("$.termination.terminatedAt").value("2027-06-30T15:00:00Z"));
    }

    @Test
    void lookupRules() throws Exception {
        lookup("VS-1234-5678")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("LOOKUP_INVALID_FORMAT"));
        lookup(ConfirmationNumber.generate())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("LOOKUP_NOT_FOUND"));

        // 확인번호는 기록 중(RECORDING)에 정해지지만, 확정 전에는 공개 조회되지 않는다.
        Contract recording = recordingContract();
        assertThat(recording.getConfirmationNo()).isNotNull();
        lookup(recording.getConfirmationNo()).andExpect(status().isNotFound());
    }

    @Test
    void fileCheckComparesHashWithRecordedOriginal() throws Exception {
        Contract contract = confirmedContract();
        String recorded = contract.getCurrentVersion().getContractFile().getFileHash();
        String other = "0x" + "0".repeat(64);

        fileCheck(contract.getConfirmationNo(), recorded)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.match").value(true))
                .andExpect(jsonPath("$.uploadedFileHash").value(recorded))
                .andExpect(jsonPath("$.recordedFileHash").value(recorded));
        fileCheck(contract.getConfirmationNo(), other)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.match").value(false))
                .andExpect(jsonPath("$.uploadedFileHash").value(other))
                .andExpect(jsonPath("$.recordedFileHash").value(recorded));

        fileCheck(contract.getConfirmationNo(), recorded.toUpperCase(Locale.ROOT))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("FILE_HASH_INVALID_FORMAT"));
        fileCheck("VS-1234-5678", recorded)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("LOOKUP_INVALID_FORMAT"));
        fileCheck(ConfirmationNumber.generate(), recorded)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("LOOKUP_NOT_FOUND"));
    }

    /** 근로자가 이해했다고 응답하고 블록체인 기록까지 끝난 계약 */
    private Contract confirmedContract() throws Exception {
        Contract contract = recordingContract();
        ChainRecord record = em.createQuery(
                        "select r from ChainRecord r where r.contractVersion.contract.id = :id", ChainRecord.class)
                .setParameter("id", contract.getId())
                .getSingleResult();
        chainRecordWorker.record(new ChainRecordRequestedEvent(contract.getId(), record.getId()));
        return reload(contract);
    }

    /** 근로자가 이해했다고 응답해 블록체인 기록을 시작한 계약 (항목 수정 포함) */
    private Contract recordingContract() throws Exception {
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

    private ResultActions lookup(String confirmationNo) throws Exception {
        return mockMvc.perform(get("/api/v1/lookup/" + confirmationNo));
    }

    private ResultActions fileCheck(String confirmationNo, String fileHash) throws Exception {
        return mockMvc.perform(post("/api/v1/lookup/" + confirmationNo + "/file-check")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fileHash\":\"" + fileHash + "\"}"));
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
