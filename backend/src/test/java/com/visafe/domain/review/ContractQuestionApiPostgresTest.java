package com.visafe.domain.review;

import static org.hamcrest.Matchers.containsString;
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
import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.contract.extraction.ContractExtractionWorker;
import com.visafe.domain.contract.extraction.ExtractionRequestedEvent;
import com.visafe.domain.member.repository.MemberRepository;
import com.visafe.domain.review.answerer.ContractQuestionAnswerer;
import com.visafe.domain.translation.entity.Translation;
import com.visafe.domain.translation.translator.ContractTranslationWorker;
import com.visafe.domain.translation.translator.TranslationRequestedEvent;
import com.visafe.global.storage.FileStorage;
import com.visafe.support.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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
 * 질문 목록(W3)과 질문하기(W4)를 실제 PostgreSQL로 확인한다. 답변은 stub 답변기 결과를 쓴다.
 * 계약은 API로 번역까지 진행시키고 추출·번역은 워커를 직접 호출해 실행한다. (S3는 메모리 가짜)
 */
@PostgresIntegrationTest
@AutoConfigureMockMvc
class ContractQuestionApiPostgresTest {

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
    private ContractQuestionAnswerer answerer;

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
    void askAnswersWithCitedClauseAndListsQuestionsInOrder() throws Exception {
        Contract contract = translatedContract();

        ask(contract, workerToken, "연장 야간 근로하면 돈을 더 받나요?")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.questionId").isNumber())
                .andExpect(jsonPath("$.language").value("vi"))
                .andExpect(jsonPath("$.question").value("연장 야간 근로하면 돈을 더 받나요?"))
                .andExpect(jsonPath("$.answer").value("[vi] 계약서 제9조를 확인해 주세요."))
                .andExpect(jsonPath("$.referredToCenter").value(false))
                .andExpect(jsonPath("$.citations.length()").value(1))
                .andExpect(jsonPath("$.citations[0].clauseNo").value("제9조"))
                .andExpect(jsonPath("$.citations[0].pageNo").value(2))
                .andExpect(jsonPath("$.citations[0].quotedText").value(containsString("연장·야간·휴일근로")))
                .andExpect(jsonPath("$.askedAt").isNotEmpty())
                .andExpect(jsonPath("$.answeredAt").isNotEmpty());
        ask(contract, workerToken, "  숙소 제공되나요?  ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.question").value("숙소 제공되나요?"))
                .andExpect(jsonPath("$.citations[0].clauseNo").value("제8조"));

        listQuestions(contract, workerToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].question").value(startsWith("연장")))
                .andExpect(jsonPath("$[0].citations[0].clauseNo").value("제9조"))
                .andExpect(jsonPath("$[1].question").value("숙소 제공되나요?"));
    }

    @Test
    void answererFailureKeepsQuestionWithoutAnswer() throws Exception {
        Contract contract = translatedContract();
        doThrow(new IllegalStateException("LLM 시간 초과")).when(answerer).answer(any());

        ask(contract, workerToken, "야근하면 돈을 더 받나요?")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.answer").isEmpty())
                .andExpect(jsonPath("$.answeredAt").isEmpty())
                .andExpect(jsonPath("$.citations.length()").value(0));
        listQuestions(contract, workerToken)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].answer").isEmpty());
    }

    @Test
    void questionRules() throws Exception {
        Contract submitted = submittedContract();
        ask(submitted, workerToken, "야근하면 돈을 더 받나요?")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("TRANSLATION_NOT_READY"));

        Contract contract = translatedContract();
        ask(contract, workerToken, "   ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_INVALID_REQUEST"));
        ask(contract, workerToken, "가".repeat(501))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_INVALID_REQUEST"));
        ask(contract, workerToken, "가".repeat(500)).andExpect(status().isCreated());
        ask(contract, employerToken, "야근하면 돈을 더 받나요?").andExpect(status().isForbidden());
        listQuestions(contract, employerToken).andExpect(status().isForbidden());
        ask(contract, login("worker2@demo.visafe.test"), "야근하면 돈을 더 받나요?")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CONTRACT_NOT_FOUND"));
    }

    private Contract translatedContract() throws Exception {
        Contract contract = submittedContract();
        translationWorker.translate(new TranslationRequestedEvent(
                contract.getId(), latestTranslation(contract).getId()));
        return reload(contract);
    }

    /** 추출 후 비어 있던 필수 항목(휴게시간)을 채워 제출한 계약 */
    private Contract submittedContract() throws Exception {
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

    private ResultActions ask(Contract contract, String token, String question) throws Exception {
        return mockMvc.perform(post("/api/v1/contracts/" + contract.getId() + "/questions")
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\": \"" + question + "\"}"));
    }

    private ResultActions listQuestions(Contract contract, String token) throws Exception {
        return mockMvc.perform(
                get("/api/v1/contracts/" + contract.getId() + "/questions").header(HttpHeaders.AUTHORIZATION, token));
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
