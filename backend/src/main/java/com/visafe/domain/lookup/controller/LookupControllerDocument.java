package com.visafe.domain.lookup.controller;

import com.visafe.domain.lookup.dto.request.FileCheckRequest;
import com.visafe.domain.lookup.dto.response.FileCheckResponse;
import com.visafe.domain.lookup.dto.response.LookupResponse;
import com.visafe.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Lookup", description = "확인번호 공개 조회 (로그인 없음)")
public interface LookupControllerDocument {

    @Operation(
            summary = "확인번호 조회",
            description = "확정·종료된 계약의 공개 정보(근로시간·계약기간, 무결성, 블록체인 기록, 공개 이력)를 조회한다. 임금은 공개하지 않고 근로자 이름은 가린다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = LookupResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "LOOKUP_INVALID_FORMAT(확인번호 형식이 잘못됨)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "LOOKUP_NOT_FOUND(확인번호에 해당하는 계약 없음)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<LookupResponse> lookup(@PathVariable String confirmationNo);

    @Operation(summary = "계약서 파일 대조", description = "브라우저가 계산한 파일의 keccak256을 확정 기록의 원본 파일 해시와 비교한다. 파일은 서버로 보내지 않는다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "대조 성공 (일치 여부는 응답 본문에 담음)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = FileCheckResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description =
                        "LOOKUP_INVALID_FORMAT(확인번호 형식이 잘못됨) · FILE_HASH_INVALID_FORMAT(파일 해시 형식이 잘못됨) · COMMON_INVALID_REQUEST_TYPE(본문 형식이 잘못됨)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "LOOKUP_NOT_FOUND(확인번호에 해당하는 계약 없음)",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<FileCheckResponse> checkFile(
            @PathVariable String confirmationNo, @RequestBody FileCheckRequest request);
}
