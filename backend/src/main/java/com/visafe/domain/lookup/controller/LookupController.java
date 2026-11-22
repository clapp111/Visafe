package com.visafe.domain.lookup.controller;

import com.visafe.domain.lookup.dto.request.FileCheckRequest;
import com.visafe.domain.lookup.dto.response.FileCheckResponse;
import com.visafe.domain.lookup.dto.response.LookupResponse;
import com.visafe.domain.lookup.service.LookupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Lookup", description = "확인번호 공개 조회 (로그인 없음)")
@RestController
@RequestMapping("/api/v1/lookup")
@RequiredArgsConstructor
public class LookupController {

    private final LookupService lookupService;

    @Operation(
            summary = "확인번호 조회",
            description = "확정·종료된 계약의 공개 정보(근로시간·계약기간, 무결성, 블록체인 기록, 공개 이력)를 조회한다. 임금은 공개하지 않고 근로자 이름은 가린다.")
    @GetMapping("/{confirmationNo}")
    public LookupResponse lookup(@PathVariable String confirmationNo) {
        return lookupService.lookup(confirmationNo);
    }

    @Operation(summary = "계약서 파일 대조", description = "브라우저가 계산한 파일의 keccak256을 확정 기록의 원본 파일 해시와 비교한다. 파일은 서버로 보내지 않는다.")
    @PostMapping("/{confirmationNo}/file-check")
    public FileCheckResponse checkFile(@PathVariable String confirmationNo, @RequestBody FileCheckRequest request) {
        return lookupService.checkFile(confirmationNo, request);
    }
}
