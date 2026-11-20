package com.visafe.global.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void restApiExceptionReturnsStatusErrorCodeAndDefaultMessage() throws Exception {
        mockMvc.perform(get("/test/stage-conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.errorCode").value("CONTRACT_STAGE_CONFLICT"))
                .andExpect(jsonPath("$.message").value(CustomErrorCode.CONTRACT_STAGE_CONFLICT.getMessage()));
    }

    @Test
    void restApiExceptionWithCustomMessageKeepsErrorCode() throws Exception {
        mockMvc.perform(get("/test/terms-missing"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value(422))
                .andExpect(jsonPath("$.errorCode").value("TERMS_REQUIRED_MISSING"))
                .andExpect(jsonPath("$.message").value("비어 있는 필수 항목: breakMinutes"));
    }

    @Test
    void unexpectedExceptionReturns500WithErrorCode() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.errorCode").value("COMMON_INTERNAL_SERVER_ERROR"));
    }

    @Test
    void uploadSizeExceededReturns413FileTooLarge() throws Exception {
        mockMvc.perform(get("/test/too-large"))
                .andExpect(status().isContentTooLarge())
                .andExpect(jsonPath("$.code").value(413))
                .andExpect(jsonPath("$.errorCode").value("FILE_TOO_LARGE"));
    }

    @Test
    void unsupportedMethodReturns405WithAllowHeader() throws Exception {
        mockMvc.perform(post("/test/get-only"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "GET"))
                .andExpect(jsonPath("$.code").value(405))
                .andExpect(jsonPath("$.errorCode").value("COMMON_METHOD_NOT_ALLOWED"));
    }

    @Test
    void unsupportedContentTypeReturns415WithAcceptHeader() throws Exception {
        mockMvc.perform(post("/test/json-only")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(header().string("Accept", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.code").value(415))
                .andExpect(jsonPath("$.errorCode").value("COMMON_UNSUPPORTED_MEDIA_TYPE"));
    }

    @RestController
    static class TestController {

        @GetMapping("/test/get-only")
        String getOnly() {
            return "ok";
        }

        @PostMapping(value = "/test/json-only", consumes = MediaType.APPLICATION_JSON_VALUE)
        String jsonOnly(@RequestBody String body) {
            return body;
        }

        @GetMapping("/test/stage-conflict")
        String stageConflict() {
            throw new RestApiException(CustomErrorCode.CONTRACT_STAGE_CONFLICT);
        }

        @GetMapping("/test/terms-missing")
        String termsMissing() {
            throw new RestApiException(CustomErrorCode.TERMS_REQUIRED_MISSING, "비어 있는 필수 항목: breakMinutes");
        }

        @GetMapping("/test/too-large")
        String tooLarge() {
            throw new MaxUploadSizeExceededException(10L * 1024 * 1024);
        }

        @GetMapping("/test/unexpected")
        String unexpected() {
            throw new IllegalStateException("boom");
        }
    }
}
