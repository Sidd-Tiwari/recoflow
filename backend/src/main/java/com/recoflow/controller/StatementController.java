package com.recoflow.controller;

import com.recoflow.config.TenantContext;
import com.recoflow.entity.StatementFile;
import com.recoflow.entity.Transaction;
import com.recoflow.repository.StatementFileRepository;
import com.recoflow.repository.TransactionRepository;
import com.recoflow.service.StatementImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/statements")
@RequiredArgsConstructor
public class StatementController {

    private final StatementImportService importService;
    private final StatementFileRepository statementFileRepository;
    private final TransactionRepository transactionRepository;

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_ACCOUNTANT')")
    public ResponseEntity<StatementFile> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(importService.upload(file));
    }

    @GetMapping
    public ResponseEntity<Page<StatementFile>> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(statementFileRepository
            .findAllByTenantTenantIdOrderByCreatedAtDesc(TenantContext.getTenantId(),
                PageRequest.of(page, size)));
    }

    @GetMapping("/{fileId}/status")
    public ResponseEntity<StatementFile> status(@PathVariable UUID fileId) {
        return ResponseEntity.ok(statementFileRepository
            .findByFileIdAndTenantTenantId(fileId, TenantContext.getTenantId())
            .orElseThrow(() -> new RuntimeException("File not found")));
    }

    @GetMapping("/{fileId}/transactions")
    public ResponseEntity<Page<Transaction>> transactions(
        @PathVariable UUID fileId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(transactionRepository
            .findAllByStatementFileFileIdAndTenantTenantId(fileId,
                TenantContext.getTenantId(), PageRequest.of(page, size)));
    }
}
