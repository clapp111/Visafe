package com.visafe.domain.member.controller;

import com.visafe.domain.member.dto.response.WorkerSearchResponse;
import com.visafe.domain.member.service.WorkerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Worker", description = "근로자")
@RestController
@RequestMapping("/api/v1/workers")
@RequiredArgsConstructor
public class WorkerController {

    private final WorkerService workerService;

    @Operation(
            summary = "근로자 검색",
            description = "확인 요청을 보낼 근로자를 이름 앞부분으로 찾는다. 대소문자와 베트남어 성조 부호를 무시하고 최대 20건을 돌려주며, 고용주만 호출할 수 있다.")
    @PreAuthorize("hasRole('EMPLOYER')")
    @GetMapping
    public List<WorkerSearchResponse> search(
            @Parameter(description = "이름 검색어 (2글자 이상)", example = "Nguyen") @RequestParam String name) {
        return workerService.searchByName(name);
    }
}
