package com.visafe.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ErrorResponseWriterTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final ErrorResponseWriter writer = new ErrorResponseWriter(jsonMapper);

    @Test
    void writesSameFormatAsExceptionHandler() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        writer.write(response, CustomErrorCode.JWT_EXPIRED);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        JsonNode body = jsonMapper.readTree(response.getContentAsString());
        assertThat(body.get("code").asInt()).isEqualTo(401);
        assertThat(body.get("errorCode").asString()).isEqualTo("JWT_EXPIRED");
        assertThat(body.get("message").asString()).isEqualTo(CustomErrorCode.JWT_EXPIRED.getMessage());
    }
}
