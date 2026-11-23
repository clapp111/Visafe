package com.visafe.domain.member.controller;

import com.visafe.domain.member.dto.response.WorkerSearchResponse;
import com.visafe.domain.member.service.WorkerService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workers")
@RequiredArgsConstructor
public class WorkerController implements WorkerControllerDocument {

    private final WorkerService workerService;

    @Override
    @PreAuthorize("hasRole('EMPLOYER')")
    @GetMapping
    public ResponseEntity<List<WorkerSearchResponse>> search(@RequestParam String name) {
        return ResponseEntity.ok(workerService.searchByName(name));
    }
}
