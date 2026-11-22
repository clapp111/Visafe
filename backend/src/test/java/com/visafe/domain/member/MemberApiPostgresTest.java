package com.visafe.domain.member;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.visafe.support.PostgresIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** 화면 언어 변경(A5)과 근로자 검색(A6)을 로그인부터 응답까지 실제 PostgreSQL로 확인한다. (데모 시드 계정 사용) */
@PostgresIntegrationTest
@AutoConfigureMockMvc
class MemberApiPostgresTest {

    private static final String PASSWORD = "visafe-demo-1234";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void changeUiLanguageReturnsMemberAndPersists() throws Exception {
        String token = login("employer@demo.visafe.test");

        mockMvc.perform(patch("/api/v1/members/me")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"uiLanguage\":\"vi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("employer@demo.visafe.test"))
                .andExpect(jsonPath("$.role").value("EMPLOYER"))
                .andExpect(jsonPath("$.uiLanguage").value("vi"));

        mockMvc.perform(get("/api/v1/members/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.uiLanguage").value("vi"));
    }

    @Test
    void unsupportedUiLanguageIsRejected() throws Exception {
        String token = login("worker1@demo.visafe.test");

        mockMvc.perform(patch("/api/v1/members/me")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"uiLanguage\":\"en\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_INVALID_REQUEST"));
    }

    @Test
    void searchIgnoresVietnameseTonesAndCaseAndShowsBothNamesakes() throws Exception {
        String token = login("employer@demo.visafe.test");

        mockMvc.perform(get("/api/v1/workers").param("name", "nguyen").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Nguyễn Văn An"))
                // 동명이인은 생년월일 순서로, 생년월일과 외국인등록번호 뒷자리로 구분한다.
                .andExpect(jsonPath("$[0].birthDate").value("1998-04-17"))
                .andExpect(jsonPath("$[0].alienRegNoLast4").value("4821"))
                .andExpect(jsonPath("$[1].birthDate").value("2001-09-03"))
                .andExpect(jsonPath("$[1].alienRegNoLast4").value("1357"))
                .andExpect(jsonPath("$[0].nationality").value("VN"))
                .andExpect(jsonPath("$[0].memberId").isNumber());
    }

    @Test
    void searchMatchesOnlyNamePrefixAndOnlyWorkers() throws Exception {
        String token = login("employer@demo.visafe.test");

        mockMvc.perform(get("/api/v1/workers").param("name", "Le Thi").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$[*].name").value(Matchers.contains("Lê Thị Hoa")));
        // 이름 중간("Van")이나 고용주 이름("김민")으로는 찾지 않는다.
        mockMvc.perform(get("/api/v1/workers").param("name", "Van").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/v1/workers").param("name", "김민").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.length()").value(0));
        // %, _는 와일드카드가 아니다.
        mockMvc.perform(get("/api/v1/workers").param("name", "%%").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shortQueryIsRejected() throws Exception {
        String token = login("employer@demo.visafe.test");

        mockMvc.perform(get("/api/v1/workers").param("name", " N ").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_INVALID_PARAMETER"));
        mockMvc.perform(get("/api/v1/workers").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_INVALID_PARAMETER"));
    }

    @Test
    void onlyEmployersCanSearchWorkers() throws Exception {
        mockMvc.perform(get("/api/v1/workers")
                        .param("name", "Nguyen")
                        .header(HttpHeaders.AUTHORIZATION, login("worker1@demo.visafe.test")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("AUTH_FORBIDDEN"));
        mockMvc.perform(get("/api/v1/workers")
                        .param("name", "Nguyen")
                        .header(HttpHeaders.AUTHORIZATION, login("admin@demo.visafe.test")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/workers").param("name", "Nguyen"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_UNAUTHORIZED"));
    }

    /** 데모 계정으로 로그인해 Authorization 헤더 값을 돌려준다. */
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
