package com.recoflow.controller;

import com.recoflow.dto.request.ManualReconRequest;
import com.recoflow.entity.Reconciliation;
import com.recoflow.enums.ReconStatus;
import com.recoflow.service.ReconciliationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reconciliations")
@RequiredArgsConstructor
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    @GetMapping
    public ResponseEntity<Page<Reconciliation>> list(
        @RequestParam(required = false) ReconStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reconciliationService.list(status,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "confidence"))));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_ACCOUNTANT')")
    public ResponseEntity<Reconciliation> confirm(@PathVariable UUID id) {
        return ResponseEntity.ok(reconciliationService.confirm(id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_ACCOUNTANT')")
    public ResponseEntity<Reconciliation> reject(@PathVariable UUID id,
                                                  @RequestParam(required = false) String notes) {
        return ResponseEntity.ok(reconciliationService.reject(id, notes));
    }

    @PostMapping("/manual")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_ACCOUNTANT')")
    public ResponseEntity<Reconciliation> manual(@Valid @RequestBody ManualReconRequest req) {
        return ResponseEntity.ok(reconciliationService.createManual(req));
    }
}
