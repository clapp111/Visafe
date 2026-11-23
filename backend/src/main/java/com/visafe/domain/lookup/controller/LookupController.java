package com.visafe.domain.lookup.controller;

import com.visafe.domain.lookup.dto.request.FileCheckRequest;
import com.visafe.domain.lookup.dto.response.FileCheckResponse;
import com.visafe.domain.lookup.dto.response.LookupResponse;
import com.visafe.domain.lookup.service.LookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lookup")
@RequiredArgsConstructor
public class LookupController implements LookupControllerDocument {

    private final LookupService lookupService;

    @Override
    @GetMapping("/{confirmationNo}")
    public ResponseEntity<LookupResponse> lookup(@PathVariable String confirmationNo) {
        return ResponseEntity.ok(lookupService.lookup(confirmationNo));
    }

    @Override
    @PostMapping("/{confirmationNo}/file-check")
    public ResponseEntity<FileCheckResponse> checkFile(
            @PathVariable String confirmationNo, @RequestBody FileCheckRequest request) {
        return ResponseEntity.ok(lookupService.checkFile(confirmationNo, request));
    }
}
