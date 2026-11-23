package com.visafe.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.jayway.jsonpath.JsonPath;
import com.visafe.support.PostgresIntegrationTest;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Swagger 문서(/v3/api-docs)의 성공 응답 코드가 실제 응답 코드와 같고, 로그인이 필요한 API에 401이 문서화됐는지 확인한다.
 */
@PostgresIntegrationTest
@AutoConfigureMockMvc
class ApiDocsPostgresTest {

    private static final Map<String, String> SUCCESS_CODES = Map.ofEntries(
            Map.entry("POST /api/v1/auth/login", "200"),
            Map.entry("POST /api/v1/auth/reissue", "200"),
            Map.entry("POST /api/v1/auth/logout", "204"),
            Map.entry("GET /api/v1/members/me", "200"),
            Map.entry("PATCH /api/v1/members/me", "200"),
            Map.entry("GET /api/v1/workers", "200"),
            Map.entry("GET /api/v1/lookup/{confirmationNo}", "200"),
            Map.entry("POST /api/v1/lookup/{confirmationNo}/file-check", "200"),
            Map.entry("POST /api/v1/contracts", "201"),
            Map.entry("PUT /api/v1/contracts/{contractId}/file", "202"),
            Map.entry("POST /api/v1/contracts/{contractId}/extraction/retry", "202"),
            Map.entry("GET /api/v1/contracts/{contractId}/files/{fileId}/download-url", "200"),
            Map.entry("GET /api/v1/contracts/{contractId}/draft", "200"),
            Map.entry("PATCH /api/v1/contracts/{contractId}/draft/terms", "200"),
            Map.entry("POST /api/v1/contracts/{contractId}/submit", "202"),
            Map.entry("POST /api/v1/contracts/{contractId}/translations/retry", "202"),
            Map.entry("POST /api/v1/contracts/{contractId}/revisions", "200"),
            Map.entry("GET /api/v1/contracts", "200"),
            Map.entry("GET /api/v1/contracts/{contractId}", "200"),
            Map.entry("GET /api/v1/contracts/{contractId}/review", "200"),
            Map.entry("POST /api/v1/contracts/{contractId}/review-responses", "202"),
            Map.entry("GET /api/v1/contracts/{contractId}/questions", "200"),
            Map.entry("POST /api/v1/contracts/{contractId}/questions", "201"),
            Map.entry("POST /api/v1/contracts/{contractId}/terminations", "201"),
            Map.entry("POST /api/v1/contracts/{contractId}/terminations/{terminationId}/response", "200"),
            Map.entry("POST /api/v1/contracts/{contractId}/terminations/{terminationId}/cancel", "200"),
            Map.entry("GET /api/v1/admin/contracts", "200"),
            Map.entry("GET /api/v1/admin/contracts/{contractId}", "200"),
            Map.entry("POST /api/v1/admin/contracts/{contractId}/approve", "202"),
            Map.entry("POST /api/v1/admin/contracts/{contractId}/reject", "200"),
            Map.entry("GET /api/v1/notifications", "200"),
            Map.entry("PATCH /api/v1/notifications/{notificationId}/read", "204"),
            Map.entry("POST /api/v1/notifications/read-all", "204"));

    @Autowired
    private MockMvc mockMvc;

    @Test
    void everyOperationDocumentsItsSuccessCodeAndAuthErrors() throws Exception {
        String docs =
                mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
        Map<String, Map<String, Map<String, Object>>> paths = JsonPath.read(docs, "$.paths");

        Map<String, Set<String>> responses = paths.entrySet().stream()
                .flatMap(path -> path.getValue().entrySet().stream()
                        .map(op -> Map.entry(
                                op.getKey().toUpperCase(Locale.ROOT) + " " + path.getKey(),
                                responseCodes(op.getValue()))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        assertThat(responses.keySet()).containsExactlyInAnyOrderElementsOf(SUCCESS_CODES.keySet());
        responses.forEach((operation, codes) -> {
            assertThat(codes.stream().filter(code -> code.startsWith("2")))
                    .as(operation)
                    .containsExactly(SUCCESS_CODES.get(operation));
            if (!operation.contains("/api/v1/auth/") && !operation.contains("/api/v1/lookup/")) {
                assertThat(codes).as(operation).contains("401");
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static Set<String> responseCodes(Map<String, Object> operation) {
        return ((Map<String, Object>) operation.get("responses")).keySet();
    }
}
