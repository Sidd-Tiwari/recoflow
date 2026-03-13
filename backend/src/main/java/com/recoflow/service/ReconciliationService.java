package com.recoflow.service;

import com.recoflow.config.TenantContext;
import com.recoflow.dto.request.ManualReconRequest;
import com.recoflow.entity.*;
import com.recoflow.enums.InvoiceStatus;
import com.recoflow.enums.ReconStatus;
import com.recoflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final ReconciliationRepository reconciliationRepository;
    private final TransactionRepository transactionRepository;
    private final InvoiceRepository invoiceRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public Page<Reconciliation> list(ReconStatus status, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        if (status != null) {
            return reconciliationRepository.findAllByTenantTenantIdAndStatus(tenantId, status, pageable);
        }
        return reconciliationRepository.findAllByTenantTenantId(tenantId, pageable);
    }

    @Transactional
    public Reconciliation confirm(UUID reconId) {
        Reconciliation recon = getById(reconId);
        if (recon.getStatus() == ReconStatus.CONFIRMED) throw new IllegalStateException("Already confirmed");

        // Ensure txn isn't already confirmed to another invoice
        boolean txnAlreadyUsed = reconciliationRepository.findByTransactionTxnId(recon.getTransaction().getTxnId())
            .stream().anyMatch(r -> r.getStatus() == ReconStatus.CONFIRMED && !r.getReconId().equals(reconId));
        if (txnAlreadyUsed) throw new IllegalStateException("Transaction already reconciled");

        User confirmedBy = userRepository.getReferenceById(TenantContext.getUserId());
        recon.setStatus(ReconStatus.CONFIRMED);
        recon.setConfirmedBy(confirmedBy);
        recon.setConfirmedAt(Instant.now());
        reconciliationRepository.save(recon);

        updateInvoicePaidAmount(recon.getInvoice());

        auditService.log(confirmedBy, "RECONCILIATION_CONFIRMED", "RECONCILIATION",
            reconId.toString(), null,
            java.util.Map.of("invoiceId", recon.getInvoice().getInvoiceId(),
                             "amount", recon.getMatchedAmount()));
        return recon;
    }

    @Transactional
    public Reconciliation reject(UUID reconId, String notes) {
        Reconciliation recon = getById(reconId);
        recon.setStatus(ReconStatus.REJECTED);
        recon.setNotes(notes);
        User actor = userRepository.getReferenceById(TenantContext.getUserId());
        auditService.log(actor, "RECONCILIATION_REJECTED", "RECONCILIATION", reconId.toString(), null, null);
        return reconciliationRepository.save(recon);
    }

    @Transactional
    public Reconciliation createManual(ManualReconRequest req) {
        UUID tenantId = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.getReferenceById(tenantId);
        User actor = userRepository.getReferenceById(TenantContext.getUserId());

        Transaction txn = transactionRepository.findById(req.getTxnId())
            .filter(t -> t.getTenant().getTenantId().equals(tenantId))
            .orElseThrow(() -> new RuntimeException("Transaction not found"));

        Invoice invoice = invoiceRepository.findByInvoiceIdAndTenantTenantId(req.getInvoiceId(), tenantId)
            .orElseThrow(() -> new RuntimeException("Invoice not found"));

        Reconciliation recon = Reconciliation.builder()
            .tenant(tenant)
            .transaction(txn)
            .invoice(invoice)
            .matchedAmount(req.getMatchedAmount() != null ? req.getMatchedAmount() : txn.getAmount())
            .confidence(BigDecimal.ONE)
            .reason(List.of("manual"))
            .status(ReconStatus.CONFIRMED)
            .confirmedBy(actor)
            .confirmedAt(Instant.now())
            .notes(req.getNotes())
            .build();

        reconciliationRepository.save(recon);
        updateInvoicePaidAmount(invoice);

        auditService.log(actor, "RECONCILIATION_MANUAL", "RECONCILIATION",
            recon.getReconId().toString(), null,
            java.util.Map.of("invoiceId", invoice.getInvoiceId(), "amount", recon.getMatchedAmount()));
        return recon;
    }

    private void updateInvoicePaidAmount(Invoice invoice) {
        BigDecimal totalPaid = reconciliationRepository.sumConfirmedAmountForInvoice(invoice.getInvoiceId());
        invoice.setPaidAmount(totalPaid);

        if (totalPaid.compareTo(invoice.getTotal()) >= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus(InvoiceStatus.PARTIAL);
        }
        invoiceRepository.save(invoice);
    }

    private Reconciliation getById(UUID reconId) {
        return reconciliationRepository.findByReconIdAndTenantTenantId(reconId, TenantContext.getTenantId())
            .orElseThrow(() -> new RuntimeException("Reconciliation not found"));
    }
}
