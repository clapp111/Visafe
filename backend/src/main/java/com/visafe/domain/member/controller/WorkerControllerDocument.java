package com.visafe.domain.member.controller;

import com.visafe.domain.member.dto.response.WorkerSearchResponse;
import com.visafe.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Worker", description = "근로자")
public interface WorkerControllerDocument {

    @Operation(
            summary = "근로자 검색",
            description = "확인 요청을 보낼 근로자를 이름 앞부분으로 찾는다. 대소문자와 베트남어 성조 부호를 무시하고 최대 20건을 돌려주며, 고용주만 호출할 수 있다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "검색 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                array = @ArraySchema(schema = @Schema(implementation = WorkerSearchResponse.class)))),
        @ApiResponse(
                responseCode = "400",
                description = "COMMON_INVALID_PARAMETER(name이 없거나 2글자 미만)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description =
                        "AUTH_UNAUTHORIZED(토큰 없음) · JWT_EXPIRED(만료된 토큰) · JWT_INVALID·JWT_MALFORMED·JWT_UNSUPPORTED(유효하지 않은 토큰)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "AUTH_FORBIDDEN(EMPLOYER 권한 없음)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<List<WorkerSearchResponse>> search(
            @Parameter(description = "이름 검색어 (2글자 이상)", example = "Nguyen") @RequestParam String name);
}
